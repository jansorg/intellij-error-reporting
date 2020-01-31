package dev.ja.samples.errorReporting;

import com.intellij.openapi.application.ApplicationInfo;
import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.dsn.Dsn;

import java.util.Collection;
import java.util.Collections;

public class SentryDemo {
    private static final SentryClient sentryClient;

    static {
        // our custom factory to configure the packages
        // using sentry.properties doesn't seem to work well
        // with the Plugin classloader.
        // the factory allows to customize all Sentry clients
        SentryClientFactory factory = new DefaultSentryClientFactory() {
            @Override
            protected Collection<String> getInAppFrames(Dsn dsn) {
                return Collections.singleton("ja.dev.samples.errorReporting");
            }
        };

        // fixme add your own Sentry DSN here
        sentryClient = SentryClientFactory.sentryClient("your-own-dsn", factory);
        // this is how to customize a single client
        sentryClient.addBuilderHelper(e -> {
            e.withTag("ide.build", ApplicationInfo.getInstance().getBuild().asString());
        });
    }

    public static SentryClient getSentryClient() {
        return sentryClient;
    }
}