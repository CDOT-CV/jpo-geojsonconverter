package us.dot.its.jpo.geojsonconverter.converter;

import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;

/**
 * Transfers ownership of successfully started streams to a JVM shutdown hook.
 * Failed instances are closed immediately, including those that never started.
 */
final class KafkaStreamsStarter {
    private final BiFunction<Topology, Properties, KafkaStreams> streamFactory;
    private final Consumer<Thread> registerHook;
    private final Consumer<Thread> removeHook;

    KafkaStreamsStarter() {
        this(KafkaStreams::new, Runtime.getRuntime()::addShutdownHook, Runtime.getRuntime()::removeShutdownHook);
    }

    KafkaStreamsStarter(BiFunction<Topology, Properties, KafkaStreams> streamFactory,
            Consumer<Thread> registerHook, Consumer<Thread> removeHook) {
        this.streamFactory = streamFactory;
        this.registerHook = registerHook;
        this.removeHook = removeHook;
    }

    void start(Topology topology, Properties properties) {
        start(topology, properties, null);
    }

    void start(Topology topology, Properties properties, StreamsUncaughtExceptionHandler exceptionHandler) {
        var streams = streamFactory.apply(topology, properties);
        Thread shutdownHook = null;
        boolean hookRegistered = false;
        try {
            if (exceptionHandler != null) {
                streams.setUncaughtExceptionHandler(exceptionHandler);
            }
            shutdownHook = Thread.ofVirtual().unstarted(streams::close);
            registerHook.accept(shutdownHook);
            hookRegistered = true;
            streams.start();
        } catch (RuntimeException | Error failure) {
            if (hookRegistered) {
                try {
                    removeHook.accept(shutdownHook);
                } catch (RuntimeException | Error cleanupFailure) {
                    suppress(failure, cleanupFailure);
                }
            }
            try {
                streams.close();
            } catch (RuntimeException | Error cleanupFailure) {
                suppress(failure, cleanupFailure);
            }
            throw failure;
        }
    }

    private static void suppress(Throwable failure, Throwable cleanupFailure) {
        if (cleanupFailure != failure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
