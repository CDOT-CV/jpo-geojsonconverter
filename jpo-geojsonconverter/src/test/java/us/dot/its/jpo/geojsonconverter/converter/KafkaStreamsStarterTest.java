package us.dot.its.jpo.geojsonconverter.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Properties;
import java.util.function.Consumer;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaStreamsStarterTest {
    @Mock
    private KafkaStreams streams;
    @Mock
    private StreamsUncaughtExceptionHandler handler;
    @Mock
    private Consumer<Thread> registerHook;
    @Mock
    private Consumer<Thread> removeHook;

    private final Topology topology = new Topology();
    private final Properties properties = new Properties();
    private KafkaStreamsStarter starter;

    @BeforeEach
    void setup() {
        starter = new KafkaStreamsStarter((actualTopology, actualProperties) -> {
            assertSame(topology, actualTopology);
            assertSame(properties, actualProperties);
            return streams;
        }, registerHook, removeHook);
    }

    @Test
    void setupFailureClosesUnstartedStream() {
        var failure = new IllegalStateException("setup failed");
        doThrow(failure).when(streams).setUncaughtExceptionHandler(handler);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> starter.start(topology, properties, handler)));

        verify(streams).close();
        verify(streams, never()).start();
        verifyNoInteractions(registerHook, removeHook);
    }

    @Test
    void hookRegistrationFailureClosesUnstartedStream() {
        var failure = new IllegalStateException("registration failed");
        doThrow(failure).when(registerHook).accept(any(Thread.class));

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> starter.start(topology, properties, handler)));

        verify(streams).close();
        verify(streams, never()).start();
        verifyNoInteractions(removeHook);
    }

    @Test
    void startFailureRemovesRegisteredHookAndClosesStream() {
        var failure = new IllegalStateException("start failed");
        doThrow(failure).when(streams).start();

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> starter.start(topology, properties, handler)));

        var hook = ArgumentCaptor.forClass(Thread.class);
        var order = inOrder(streams, registerHook, removeHook);
        order.verify(streams).setUncaughtExceptionHandler(handler);
        order.verify(registerHook).accept(hook.capture());
        order.verify(streams).start();
        order.verify(removeHook).accept(hook.getValue());
        order.verify(streams).close();
        order.verifyNoMoreInteractions();
    }

    @Test
    void successfulStreamRemainsOpenUntilShutdownHookRuns() throws InterruptedException {
        starter.start(topology, properties, handler);

        var hook = ArgumentCaptor.forClass(Thread.class);
        var order = inOrder(streams, registerHook);
        order.verify(streams).setUncaughtExceptionHandler(handler);
        order.verify(registerHook).accept(hook.capture());
        order.verify(streams).start();
        verify(streams, never()).close();
        verifyNoInteractions(removeHook);
        assertTrue(hook.getValue().isVirtual());
        assertEquals(Thread.State.NEW, hook.getValue().getState());

        hook.getValue().start();
        hook.getValue().join(5000);
        assertFalse(hook.getValue().isAlive());
        verify(streams).close();
    }

    @Test
    void streamWithoutCustomHandlerStillUsesShutdownHook() throws InterruptedException {
        starter.start(topology, properties);

        verify(streams, never()).setUncaughtExceptionHandler(any(StreamsUncaughtExceptionHandler.class));
        verify(streams).start();
        var hook = ArgumentCaptor.forClass(Thread.class);
        verify(registerHook).accept(hook.capture());
        hook.getValue().start();
        hook.getValue().join(5000);
        assertFalse(hook.getValue().isAlive());
        verify(streams).close();
        verifyNoInteractions(removeHook);
    }

    @Test
    void cleanupFailuresAreSuppressedAndCloseIsAttemptedAfterRemovalFailure() {
        var failure = new IllegalStateException("start failed");
        var removalFailure = new IllegalStateException("removal failed");
        var closeFailure = new IllegalStateException("close failed");
        doThrow(failure).when(streams).start();
        doThrow(removalFailure).when(removeHook).accept(any(Thread.class));
        doThrow(closeFailure).when(streams).close();

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> starter.start(topology, properties, handler)));

        assertArrayEquals(new Throwable[] {removalFailure, closeFailure}, failure.getSuppressed());
        verify(streams).close();
    }

    @Test
    void closeFailureIsSuppressedEvenBeforeHookRegistration() {
        var failure = new IllegalStateException("setup failed");
        var closeFailure = new IllegalStateException("close failed");
        doThrow(failure).when(streams).setUncaughtExceptionHandler(handler);
        doThrow(closeFailure).when(streams).close();

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> starter.start(topology, properties, handler)));

        assertArrayEquals(new Throwable[] {closeFailure}, failure.getSuppressed());
        verifyNoInteractions(registerHook, removeHook);
    }

    @Test
    void startupErrorAlsoClosesStreamAndKeepsOriginalError() {
        var failure = new AssertionError("start failed");
        doThrow(failure).when(streams).start();

        assertSame(failure, assertThrows(AssertionError.class,
                () -> starter.start(topology, properties, handler)));

        verify(removeHook).accept(any(Thread.class));
        verify(streams).close();
    }

    @Test
    void repeatedFailureInstanceDoesNotReplaceOriginalWithSelfSuppressionError() {
        var failure = new IllegalStateException("start and cleanup failed");
        doThrow(failure).when(streams).start();
        doThrow(failure).when(removeHook).accept(any(Thread.class));
        doThrow(failure).when(streams).close();

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> starter.start(topology, properties, handler)));

        verify(streams).close();
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    void factoryFailureDoesNotRegisterHooks() {
        var failure = new IllegalStateException("factory failed");
        starter = new KafkaStreamsStarter((actualTopology, actualProperties) -> {
            throw failure;
        }, registerHook, removeHook);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> starter.start(topology, properties, handler)));

        verifyNoInteractions(streams, registerHook, removeHook);
    }
}
