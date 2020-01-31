package dev.ja.samples.errorReporting;

import com.intellij.diagnostic.AbstractMessage;
import com.intellij.diagnostic.IdeaReportingEvent;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.Consumer;
import io.sentry.SentryClient;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * This is a sample implementation how to report exceptions
 * to a <a href="https://sentry.io/welcome/">Sentry</a> endpoint.
 *
 * @author jansorg
 */
public class SentryErrorReporter extends ErrorReportSubmitter {
    @Nullable
    @Override
    public String getPrivacyNoticeText() {
        return "Hereby you agree to <a href=\"https://www.example.com\">this privacy statement</a>";
    }

    @Nullable
    @Override
    public String getReporterAccount() {
        return "user-id";
    }

    @Override
    public void changeReporterAccount(@NotNull Component parentComponent) {
        // change it
    }

    @NotNull
    @Override
    public String getReportActionText() {
        return "Report to Author";
    }

    /**
     * Here comes the main implementation of your error reporter.
     * See the definition of the super method for more specific comments.
     *
     * @param events          The list of events to process. IntelliJ seems to always send just one event.
     * @param additionalInfo  Optional, user-provided notes
     * @param parentComponent
     * @param consumer
     * @return
     */
    @Override
    public boolean submit(@NotNull IdeaLoggingEvent[] events,
                          @Nullable String additionalInfo,
                          @NotNull Component parentComponent,
                          @NotNull Consumer<SubmittedReportInfo> consumer) {

        Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(parentComponent));

        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            EventBuilder event = new EventBuilder();
            event.withLevel(Event.Level.ERROR);
            if (getPluginDescriptor() instanceof IdeaPluginDescriptor) {
                event.withRelease(((IdeaPluginDescriptor) getPluginDescriptor()).getVersion());
            }
            // set server name to empty to avoid tracking personal data
            event.withServerName("");

            // now, attach all exceptions to the message
            Deque<SentryException> errors = new ArrayDeque<>(events.length);
            for (IdeaLoggingEvent ideaEvent : events) {
                // this is the tricky part
                // ideaEvent.throwable is a com.intellij.diagnostic.IdeaReportingEvent.TextBasedThrowable
                // This is a wrapper and is only providing the original stacktrace via 'printStackTrace(...)',
                // but not via 'getStackTrace()'.
                //
                // Sentry accesses Throwable.getStackTrace(),
                // So, we workaround this by retrieving the original exception from the data property
                if (ideaEvent instanceof IdeaReportingEvent && ideaEvent.getData() instanceof AbstractMessage) {
                    Throwable ex = ((AbstractMessage) ideaEvent.getData()).getThrowable();
                    errors.add(new SentryException(ex, ex.getStackTrace()));
                } else {
                    // ignoring this ideaEvent, you might not want to do this
                }
            }
            event.withSentryInterface(new ExceptionInterface(errors));
            // might be useful to debug the exception
            event.withExtra("last_action", IdeaLogger.ourLastActionId);

            // by default, Sentry is sending async in a background thread
            SentryClient sentry = SentryDemo.getSentryClient();
            sentry.sendEvent(event);

            ApplicationManager.getApplication().invokeLater(() -> {
                // we're a bit lazy here.
                // Alternatively, we could add a listener to the sentry client
                // to be notified if the message was successfully send
                Messages.showInfoMessage(parentComponent, "Thank you for submitting your report!", "Error Report");
                consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
            });
        }, "Sending Error to Sentry", false, project);
        return true;
    }
}
