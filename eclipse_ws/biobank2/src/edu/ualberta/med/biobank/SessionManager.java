package edu.ualberta.med.biobank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

import edu.ualberta.med.biobank.client.util.ServiceConnection;
import edu.ualberta.med.biobank.common.security.Privilege;
import edu.ualberta.med.biobank.common.security.User;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.dialogs.ChangePasswordDialog;
import edu.ualberta.med.biobank.logs.BiobankLogger;
import edu.ualberta.med.biobank.server.applicationservice.BiobankApplicationService;
import edu.ualberta.med.biobank.sourceproviders.DebugState;
import edu.ualberta.med.biobank.sourceproviders.SessionState;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.treeview.RootNode;
import edu.ualberta.med.biobank.treeview.admin.SessionAdapter;
import edu.ualberta.med.biobank.treeview.util.AdapterFactory;
import edu.ualberta.med.biobank.utils.BindingContextHelper;
import edu.ualberta.med.biobank.views.AbstractViewWithAdapterTree;
import edu.ualberta.med.biobank.views.SessionsView;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

public class SessionManager {

    public static final String BIOBANK2_CONTEXT_LOGGED_OUT = "biobank2.context.loggedOut";

    public static final String BIOBANK2_CONTEXT_LOGGED_IN = "biobank2.context.loggedIn";

    private static BiobankLogger logger = BiobankLogger
        .getLogger(SessionManager.class.getName());

    private static SessionManager instance = null;

    private SessionsView view;

    private SessionAdapter sessionAdapter;

    private RootNode rootNode;

    /**
     * Map a perspective ID to a AbstractViewWithTree instance visible when the
     * perspective is set
     */
    public Map<String, AbstractViewWithAdapterTree> possibleViewMap;

    private String currentAdministrationViewId;

    private SessionManager() {
        super();
        rootNode = new RootNode();
        possibleViewMap = new HashMap<String, AbstractViewWithAdapterTree>();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setSessionsView(SessionsView view) {
        this.view = view;
        addView(view);
        updateMenus();
    }

    public void addSession(final BiobankApplicationService appService,
        String serverName, User user, Collection<SiteWrapper> sites) {
        logger.debug("addSession: " + serverName + ", user/" + user.getLogin()
            + " numSites/" + sites.size());
        sessionAdapter = new SessionAdapter(rootNode, appService, 0,
            serverName, user);
        rootNode.addChild(sessionAdapter);

        updateMenus();

        if (sessionAdapter.getUser().passwordChangeRequired()) {
            ChangePasswordDialog dlg = new ChangePasswordDialog(PlatformUI
                .getWorkbench().getActiveWorkbenchWindow().getShell(), true);
            dlg.open();
        }

        // for key binding contexts:
        BindingContextHelper
            .activateContextInWorkbench(BIOBANK2_CONTEXT_LOGGED_IN);
        BindingContextHelper
            .deactivateContextInWorkbench(BIOBANK2_CONTEXT_LOGGED_OUT);
    }

    public void deleteSession() throws Exception {
        WritableApplicationService appService = sessionAdapter.getAppService();
        sessionAdapter = null;
        updateMenus();
        ServiceConnection.logout(appService);

        // for key binding contexts:
        BindingContextHelper
            .activateContextInWorkbench(BIOBANK2_CONTEXT_LOGGED_OUT);
        BindingContextHelper
            .deactivateContextInWorkbench(BIOBANK2_CONTEXT_LOGGED_IN);
    }

    public void updateSession() {
        Assert.isNotNull(sessionAdapter, "session adapter is null");
        sessionAdapter.performExpand();
    }

    private void updateMenus() {
        IWorkbenchWindow window = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow();
        ISourceProviderService service = (ISourceProviderService) window
            .getService(ISourceProviderService.class);

        // assign logged in state
        SessionState sessionSourceProvider = (SessionState) service
            .getSourceProvider(SessionState.LOGIN_STATE_SOURCE_NAME);
        sessionSourceProvider.setLoggedInState(sessionAdapter != null);
        sessionSourceProvider.setWebAdmin(sessionAdapter != null
            && sessionAdapter.getUser().isWebsiteAdministrator());

        // assign debug state
        DebugState debugStateSourceProvider = (DebugState) service
            .getSourceProvider(DebugState.SESSION_STATE);
        debugStateSourceProvider.setState(BioBankPlugin.getDefault()
            .isDebugging());
    }

    public SessionAdapter getSession() {
        Assert.isNotNull(sessionAdapter, "session adapter is null");
        return sessionAdapter;
    }

    public static BiobankApplicationService getAppService() {
        return getInstance().getSession().getAppService();
    }

    public static void updateAdapterTreeNode(final AdapterBase node) {
        final AbstractViewWithAdapterTree view = getCurrentAdapterViewWithTree();
        if (view != null) {
            view.getTreeViewer().update(node, null);
        }
    }

    public static void refreshTreeNode(final AdapterBase node) {
        final AbstractViewWithAdapterTree view = getCurrentAdapterViewWithTree();
        if (view != null) {
            view.getTreeViewer().refresh(node, true);
        }
    }

    public static void setSelectedNode(final AdapterBase node) {
        final AbstractViewWithAdapterTree view = getCurrentAdapterViewWithTree();
        if (view != null && node != null) {
            view.setSelectedNode(node);
        }
    }

    public static AdapterBase getSelectedNode() {
        AbstractViewWithAdapterTree view = getCurrentAdapterViewWithTree();
        if (view != null) {
            AdapterBase selectedNode = view.getSelectedNode();
            return selectedNode;
        }
        return null;
    }

    public static void openViewForm(ModelWrapper<?> wrapper) {
        AdapterBase adapter = searchFirstNode(wrapper);
        if (adapter != null) {
            adapter.performDoubleClick();
            return;
        }

        // adapter for object not yet in the tree, create new adapter
        // and load the view form
        adapter = AdapterFactory.getAdapter(wrapper);
        if (adapter != null) {
            adapter.performDoubleClick();
        }
    }

    public static AbstractViewWithAdapterTree getCurrentAdapterViewWithTree() {
        SessionManager sm = getInstance();
        return sm.possibleViewMap.get(sm.currentAdministrationViewId);
    }

    public static List<AdapterBase> searchNodes(ModelWrapper<?> wrapper) {
        AbstractViewWithAdapterTree view = getCurrentAdapterViewWithTree();
        if (view != null) {
            return view.searchNode(wrapper);
        }
        return new ArrayList<AdapterBase>();
    }

    public static AdapterBase searchFirstNode(ModelWrapper<?> wrapper) {
        List<AdapterBase> nodes = searchNodes(wrapper);
        if (nodes.size() > 0) {
            return nodes.get(0);
        }
        return null;
    }

    public RootNode getRootNode() {
        return rootNode;
    }

    public static void addView(AbstractViewWithAdapterTree view) {
        getInstance().possibleViewMap.put(view.getId(), view);
        getInstance().currentAdministrationViewId = view.getId();
    }

    public static User getUser() {
        return getInstance().getSession().getUser();
    }

    public static String getServer() {
        return getInstance().getSession().getServerName();
    }

    /**
     * Site specific only if currentSite != null
     */
    public static boolean canCreate(Class<?> clazz, SiteWrapper currentSite) {
        return getUser().hasPrivilegeOnObject(Privilege.CREATE,
            currentSite == null ? null : currentSite.getId(), clazz, null);
    }

    /**
     * Site specific only if currentSite != null
     */
    public static boolean canDelete(Class<?> clazz, SiteWrapper currentSite) {
        return getUser().hasPrivilegeOnObject(Privilege.DELETE,
            currentSite == null ? null : currentSite.getId(), clazz, null);
    }

    public static boolean canDelete(ModelWrapper<?> wrapper) {
        return wrapper.canDelete(getUser());
    }

    public static boolean canView(Class<?> clazz) {
        return getUser()
            .hasPrivilegeOnObject(Privilege.READ, null, clazz, null);
    }

    /**
     * Site specific only if currentSite != null
     */
    public static boolean canUpdate(Class<?> clazz, SiteWrapper currentSite) {
        return getUser().hasPrivilegeOnObject(Privilege.UPDATE,
            currentSite == null ? null : currentSite.getId(), clazz, null);
    }

    public boolean isConnected() {
        return sessionAdapter != null;
    }

    public static void log(String action, String details, String type)
        throws Exception {
        getAppService().logActivity(action, null, null, null, null, details,
            type);
    }

    /**
     * do an update on node holding the same wrapper than the given adapter.
     * 
     * @param canRest if true, then the node found will be reloaded from the
     *            database (might not want that if the object could be open in
     *            an entry form).
     * @param expandParent if true will expand the parent node of 'adapter'
     * 
     */
    public static void updateAllSimilarNodes(final AdapterBase adapter,
        final boolean canReset) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    // add to add the correct node if it is a new adapter:
                    AdapterBase parent = adapter.getParent();
                    if (parent != null)
                        parent.addChild(adapter);
                    List<AdapterBase> res = searchNodes(adapter
                        .getModelObject());
                    final AbstractViewWithAdapterTree view = getCurrentAdapterViewWithTree();
                    if (view != null) {
                        for (AdapterBase ab : res) {
                            if (canReset)
                                try {
                                    if (ab != adapter)
                                        ab.resetObject();
                                } catch (Exception ex) {
                                    logger.error("Problem reseting object", ex);
                                }
                            view.getTreeViewer().update(ab, null);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void setCurrentAdministrationViewId(String id) {
        getInstance().currentAdministrationViewId = id;
    }
}
