package edu.ualberta.med.biobank.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.forms.input.FormInput;

public class OpenPatientFormHandler extends AbstractHandler implements IHandler {

    private static final String EDITOR_ID_PARAM = "edu.ualberta.med.biobank.commands.patients.openPatientForm.editorId";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Map parameters = event.getParameters();
        final String editorId = (String) parameters.get(EDITOR_ID_PARAM);
        IWorkbenchWindow window = HandlerUtil
            .getActiveWorkbenchWindowChecked(event);
        try {
            IWorkbenchPage activePage = window.getActivePage();
            if (activePage == null) {
                return null;
            }
            // hide others view
            for (IViewReference ref : activePage.getViewReferences()) {
                activePage.hideView(ref);
            }
            // close opened editors
            activePage.closeAllEditors(true);
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().openEditor(
                    new FormInput(SessionManager.getInstance()
                        .getSessionAdapter()), editorId, true);
        } catch (PartInitException e) {
            throw new ExecutionException("Part could not be initialized", e); //$NON-NLS-1$
        }
        return null;
    }
}
