package edu.ualberta.med.biobank;

import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.sourceproviders.DebugState;
import edu.ualberta.med.biobank.sourceproviders.SessionState;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.treeview.RootNode;
import edu.ualberta.med.biobank.treeview.SessionAdapter;
import edu.ualberta.med.biobank.treeview.SiteAdapter;
import edu.ualberta.med.biobank.views.SessionsView;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

public class SessionManager {
    private static SessionManager instance = null;

    private static Logger log4j = Logger.getLogger(SessionManager.class
        .getName());

    private SessionsView view;

    private SessionAdapter sessionAdapter;

    private AdapterBase rootNode;

    public boolean inactiveTimeout = false;

    private final Semaphore timeoutSem = new Semaphore(100, true);

    final int TIME_OUT = 900000;

    final Runnable timeoutRunnable = new Runnable() {
        public void run() {
            try {
                timeoutSem.acquire();
                inactiveTimeout = true;
                // System.out
                // .println("startInactivityTimer_runnable: inactiveTimeout/"
                // + inactiveTimeout);

                boolean logout = BioBankPlugin.openConfirm("Inactive Timeout",
                    "The application has been inactive for "
                        + (TIME_OUT / 1000)
                        + " seconds.\n Do you want to log out?");

                if (logout) {
                    deleteSession();
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().closeAllEditors(true);
                }
                timeoutSem.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    public AdapterBase getRootNode() {
        return rootNode;
    }

    private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
        public void doubleClick(DoubleClickEvent event) {
            Object selection = event.getSelection();

            if (selection == null)
                return;

            Object element = ((StructuredSelection) selection)
                .getFirstElement();
            ((AdapterBase) element).performDoubleClick();
            view.getTreeViewer().expandToLevel(element, 1);
        }
    };

    public IDoubleClickListener getDoubleClickListener() {
        return doubleClickListener;
    }

    private SessionManager() {
        super();
        rootNode = new RootNode();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setSessionsView(SessionsView view) {
        this.view = view;
        updateMenus();
    }

    public void addSession(final WritableApplicationService appService,
        String name, String userName, List<Site> sites) {
        sessionAdapter = new SessionAdapter(rootNode, appService, 0, name,
            userName);
        rootNode.addChild(sessionAdapter);
        for (Object o : sites) {
            Site site = (Site) o;
            SiteAdapter siteNode = new SiteAdapter(sessionAdapter, site);
            sessionAdapter.addChild(siteNode);
        }
        view.getTreeViewer().expandToLevel(2);
        log4j.debug("addSession: " + name);
        startInactivityTimer();
        updateMenus();
    }

    private void startInactivityTimer() {
        try {
            timeoutSem.acquire();
            inactiveTimeout = false;
            // System.out.println("startInactivityTimer: inactiveTimeout/"
            // + inactiveTimeout);

            final Display display = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell().getDisplay();

            // this listener will be called when the events listed below happen
            Listener idleListener = new Listener() {
                public void handleEvent(Event event) {
                    try {
                        timeoutSem.acquire();
                        // System.out
                        // .println("startInactivityTimer_idleListener: inactiveTimeout/"
                        // + inactiveTimeout);
                        if (!inactiveTimeout && (sessionAdapter != null)) {
                            display.timerExec(TIME_OUT, timeoutRunnable);
                        }
                        timeoutSem.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            int[] events = { SWT.KeyDown, SWT.KeyUp, SWT.MouseDown,
                SWT.MouseMove, SWT.MouseUp };
            for (int event : events) {
                display.addFilter(event, idleListener);
            }
            display.timerExec(TIME_OUT, timeoutRunnable);
            timeoutSem.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public SessionAdapter getSessionAdapter() {
        if (sessionAdapter == null)
            return null;
        List<AdapterBase> nodes = rootNode.getChildren();
        return (SessionAdapter) nodes.get(0);
    }

    public void deleteSession() {
        rootNode.removeChild(sessionAdapter);
        sessionAdapter = null;
        updateMenus();
    }

    private void updateMenus() {
        IWorkbenchWindow window = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow();
        ISourceProviderService service = (ISourceProviderService) window
            .getService(ISourceProviderService.class);

        // assign logged in state
        SessionState sessionSourceProvider = (SessionState) service
            .getSourceProvider(SessionState.SESSION_STATE);
        sessionSourceProvider.setLoggedInState(sessionAdapter != null);

        // assign debug state
        DebugState debugStateSourceProvider = (DebugState) service
            .getSourceProvider(DebugState.SESSION_STATE);
        debugStateSourceProvider.setState(BioBankPlugin.getDefault()
            .isDebugging());
    }

    public SessionAdapter getSession() {
        return sessionAdapter;
    }

    public TreeViewer getTreeViewer() {
        return view.getTreeViewer();
    }

    // public TreeFilter getTreeFilter() {
    // return view.getFilter();
    // }

    public static Logger getLogger() {
        return log4j;
    }

    public void openViewForm(Object o) {

    }
}
