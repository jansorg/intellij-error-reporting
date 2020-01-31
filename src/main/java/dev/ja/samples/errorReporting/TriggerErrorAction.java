package dev.ja.samples.errorReporting;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class TriggerErrorAction extends AnAction {
    public TriggerErrorAction() {
        super("Error Reporting: Trigger Exception");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        throw new IllegalStateException("this is a bug!");
    }
}
