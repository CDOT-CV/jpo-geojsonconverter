package us.dot.its.jpo.geojsonconverter.converter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class KafkaStreamsStarterTest {
    private final KafkaStreams streams = mock(KafkaStreams.class);
    private final Topology topology = new Topology();
    private final Properties properties = new Properties();
    private final List<Thread> registeredHooks = new ArrayList<>();
    private final Consumer<Thread> removeHook = mock(Consumer.class);
    private final StreamsUncaughtExceptionHandler handler = mock(StreamsUncaughtExceptionHandler.class);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void successfulStartTransfersCleanupToShutdownHook(boolean withHandler) throws InterruptedException {
        var starter = new KafkaStreamsStarter((actualTopology, actualProperties) -> {
            assertSame(topology, actualTopology);
            assertSame(properties, actualProperties);
            return streams;
        }, registeredHooks::add, removeHook);

        if (withHandler) {
            starter.start(topology, properties, handler);
            var order = inOrder(streams);
            order.verify(streams).setUncaughtExceptionHandler(handler);
            order.verify(streams).start();
        } else {
            starter.start(topology, properties);
            verify(streams, never()).setUncaughtExceptionHandler(any(StreamsUncaughtExceptionHandler.class));
            verify(streams).start();
        }
        verify(streams, never()).close();
        verifyNoInteractions(removeHook);
        assertEquals(1, registeredHooks.size());

        Thread hook = registeredHooks.getFirst();
        hook.start();
        hook.join();
        verify(streams).close();
    }

    @ParameterizedTest
    @MethodSource("startupFailures")
    void failedStartClosesStreamsAndRemovesOnlyRegisteredHooks(String phase, Throwable failure) {
        Consumer<Thread> registerHook = hook -> {
            registeredHooks.add(hook);
            if (phase.equals("registration")) {
                throwUnchecked(failure);
            }
        };
        if (phase.equals("handler")) {
            doThrow(failure).when(streams).setUncaughtExceptionHandler(handler);
        } else if (phase.equals("start")) {
            doThrow(failure).when(streams).start();
        }
        var starter = new KafkaStreamsStarter((unusedTopology, unusedProperties) -> streams, registerHook, removeHook);

        assertSame(failure, assertThrows(failure.getClass(), () -> starter.start(topology, properties, handler)));

        verify(streams).close();
        if (phase.equals("start")) {
            verify(removeHook).accept(registeredHooks.getFirst());
        } else {
            verifyNoInteractions(removeHook);
            verify(streams, never()).start();
        }
        assertEquals(0, failure.getSuppressed().length);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleanupFailuresAreSuppressedWithoutReplacingOriginalFailure(boolean error) {
        Throwable failure = error ? new LinkageError("start") : new IllegalStateException("start");
        Throwable removalFailure = error ? new LinkageError("remove hook") : new IllegalStateException("remove hook");
        Throwable closeFailure = error ? new LinkageError("close") : new IllegalStateException("close");
        doThrow(failure).when(streams).start();
        doThrow(removalFailure).when(removeHook).accept(any(Thread.class));
        doThrow(closeFailure).when(streams).close();
        var starter = new KafkaStreamsStarter((unusedTopology, unusedProperties) -> streams,
                registeredHooks::add, removeHook);

        assertSame(failure, assertThrows(failure.getClass(), () -> starter.start(topology, properties)));

        assertArrayEquals(new Throwable[] {removalFailure, closeFailure}, failure.getSuppressed());
        var order = inOrder(removeHook, streams);
        order.verify(streams).start();
        order.verify(removeHook).accept(registeredHooks.getFirst());
        order.verify(streams).close();
    }

    @Test
    void cleanupRethrowingOriginalFailureDoesNotAttemptSelfSuppression() {
        var failure = new IllegalStateException("same failure");
        doThrow(failure).when(streams).start();
        doThrow(failure).when(removeHook).accept(any(Thread.class));
        doThrow(failure).when(streams).close();
        var starter = new KafkaStreamsStarter((unusedTopology, unusedProperties) -> streams,
                registeredHooks::add, removeHook);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> starter.start(topology, properties)));

        assertEquals(0, failure.getSuppressed().length);
        verify(streams).close();
    }

    private static Stream<Arguments> startupFailures() {
        return Stream.of("handler", "registration", "start").flatMap(phase -> Stream.of(
                Arguments.of(phase, new IllegalStateException(phase)),
                Arguments.of(phase, new LinkageError(phase))));
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        throw (RuntimeException) failure;
    }
}
