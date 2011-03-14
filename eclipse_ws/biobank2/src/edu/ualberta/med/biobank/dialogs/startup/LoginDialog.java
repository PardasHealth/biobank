package edu.ualberta.med.biobank.dialogs.startup;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import edu.ualberta.med.biobank.BiobankPlugin;
import edu.ualberta.med.biobank.Messages;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.CenterWrapper;
import edu.ualberta.med.biobank.helpers.SessionHelper;
import edu.ualberta.med.biobank.logs.BiobankLogger;
import edu.ualberta.med.biobank.preferences.PreferenceConstants;
import edu.ualberta.med.biobank.rcp.Application;
import edu.ualberta.med.biobank.rcp.perspective.MainPerspective;
import edu.ualberta.med.biobank.validators.AbstractValidator;
import edu.ualberta.med.biobank.validators.NonEmptyStringValidator;

public class LoginDialog extends TitleAreaDialog {

    private DataBindingContext dbc;

    private ArrayList<String> servers;

    private ArrayList<String> userNames;

    private Combo serverWidget;

    private Combo userNameWidget;

    private Text passwordWidget;

    private static final String LAST_SERVER = "lastServer"; //$NON-NLS-1$

    private static final String SAVED_USER_NAMES = "savedUserNames"; //$NON-NLS-1$

    private static final String USER_NAME = "userName"; //$NON-NLS-1$

    private static final String LAST_USER_NAME = "lastUserName"; //$NON-NLS-1$

    private static final String DEFAULT_SECURE_PORT = "8443"; //$NON-NLS-1$

    private static final String DEFAULT_UNSECURE_PREFIX = "http://"; //$NON-NLS-1$

    private static final BiobankLogger logger = BiobankLogger
        .getLogger(LoginDialog.class.getName());

    public Preferences pluginPrefs = null;

    private Button secureConnectionButton;

    private Authentication authentication;

    private Boolean okButtonEnabled;

    private boolean setupFinished = false;

    private Button superAdminWidget;

    private boolean superAdminMode = false;

    public LoginDialog(Shell parentShell) {
        super(parentShell);

        authentication = new Authentication();

        dbc = new DataBindingContext();

        servers = new ArrayList<String>();
        userNames = new ArrayList<String>();

        pluginPrefs = new InstanceScope().getNode(Application.PLUGIN_ID);
        Preferences prefsUserNames = pluginPrefs.node(SAVED_USER_NAMES);

        IPreferenceStore prefsStore = BiobankPlugin.getDefault()
            .getPreferenceStore();

        String serverList = prefsStore
            .getString(PreferenceConstants.SERVER_LIST);
        StringTokenizer st = new StringTokenizer(serverList, "\n"); //$NON-NLS-1$
        while (st.hasMoreTokens()) {
            servers.add(st.nextToken());
        }

        try {
            String[] userNodeNames = prefsUserNames.childrenNames();
            for (String userNodeName : userNodeNames) {
                Preferences node = prefsUserNames.node(userNodeName);
                userNames.add(node.get(USER_NAME, "")); //$NON-NLS-1$
            }
        } catch (BackingStoreException e) {
            logger.error("Could not get " + USER_NAME + " preference", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.getString("LoginDialog.main.title")); //$NON-NLS-1$
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle(Messages.getString("LoginDialog.title")); //$NON-NLS-1$
        setTitleImage(BiobankPlugin.getDefault().getImageRegistry()
            .get(BiobankPlugin.IMG_COMPUTER_KEY));
        setMessage(Messages.getString("LoginDialog.description")); //$NON-NLS-1$
        return contents;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control contents = super.createButtonBar(parent);
        if (okButtonEnabled != null) {
            // in case the binding wanted to modify it before its creation
            setOkButtonEnabled(okButtonEnabled);
        }
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite parentComposite = (Composite) super.createDialogArea(parent);

        Composite contents = new Composite(parentComposite, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        contents.setLayout(layout);
        contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        String lastServer = pluginPrefs.get(LAST_SERVER, ""); //$NON-NLS-1$
        NonEmptyStringValidator validator = new NonEmptyStringValidator(
            Messages.getString("LoginDialog.field.server.validation.msg")); //$NON-NLS-1$
        serverWidget = createWritableCombo(
            contents,
            Messages.getString("LoginDialog.field.server.label"), //$NON-NLS-1$
            servers.toArray(new String[0]),
            Authentication.SERVER_PROPERTY_NAME, lastServer, validator);

        NonEmptyStringValidator userNameValidator = null;
        NonEmptyStringValidator passwordValidator = null;
        if (BiobankPlugin.getDefault().isDebugging()) {
            new Label(contents, SWT.NONE);
            secureConnectionButton = new Button(contents, SWT.CHECK);
            secureConnectionButton.setText(Messages
                .getString("LoginDialog.field.secure.connection")); //$NON-NLS-1$
            secureConnectionButton.setSelection(lastServer
                .contains(DEFAULT_SECURE_PORT));

            serverWidget.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String lastServer = serverWidget.getText();
                    secureConnectionButton.setSelection(lastServer
                        .contains(DEFAULT_SECURE_PORT));
                }
            });
        } else {
            userNameValidator = new NonEmptyStringValidator(
                Messages.getString("LoginDialog.field.user.validation.msg")); //$NON-NLS-1$
            passwordValidator = new NonEmptyStringValidator(
                Messages.getString("LoginDialog.field.password.validaton.msg")); //$NON-NLS-1$
        }

        userNameWidget = createWritableCombo(
            contents,
            Messages.getString("LoginDialog.field.user.label"), //$NON-NLS-1$
            userNames.toArray(new String[0]),
            Authentication.USERNAME_PROPERTY_NAME,
            pluginPrefs.get(LAST_USER_NAME, ""), userNameValidator); //$NON-NLS-1$

        passwordWidget = createPassWordText(contents,
            Messages.getString("LoginDialog.field.password.label"), //$NON-NLS-1$
            Authentication.PASSWORD_PROPERTY_NAME, passwordValidator);

        new Label(contents, SWT.NONE);
        superAdminWidget = new Button(contents, SWT.CHECK);
        superAdminWidget.setText(Messages
            .getString("LoginDialog.field.superAdmin")); //$NON-NLS-1$

        bindChangeListener();

        setupFinished = true;

        return contents;
    }

    private Text createPassWordText(Composite parent, String labelText,
        String propertyObserved, AbstractValidator validator) {
        createLabel(parent, labelText);
        Text text = new Text(parent, SWT.BORDER | SWT.PASSWORD);
        arrangeAndBindControl(text, validator,
            SWTObservables.observeText(text, SWT.Modify), propertyObserved);
        return text;
    }

    private void arrangeAndBindControl(Control control,
        AbstractValidator validator, ISWTObservableValue observable,
        String propertyObserved) {
        control.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
            false));
        UpdateValueStrategy uvs = null;
        if (validator != null) {
            uvs = new UpdateValueStrategy();
            uvs.setAfterGetValidator(validator);
        }
        dbc.bindValue(observable,
            PojoObservables.observeValue(authentication, propertyObserved),
            uvs, null);
    }

    private Label createLabel(Composite parent, String labelText) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText + ":"); //$NON-NLS-1$
        label.setLayoutData(new GridData(GridData.END, GridData.CENTER, false,
            false));
        return label;
    }

    private Combo createWritableCombo(Composite parent, String labelText,
        String[] values, String propertyObserved, String selection,
        AbstractValidator validator) {
        createLabel(parent, labelText);

        Combo combo = new Combo(parent, SWT.BORDER);
        combo.setItems(values);
        if (selection != null) {
            combo.select(combo.indexOf(selection));
        }
        arrangeAndBindControl(combo, validator,
            SWTObservables.observeSelection(combo), propertyObserved);
        return combo;
    }

    protected void bindChangeListener() {
        final IObservableValue statusObservable = new WritableValue();
        statusObservable.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent event) {
                IObservableValue validationStatus = (IObservableValue) event
                    .getSource();
                IStatus status = (IStatus) validationStatus.getValue();
                if (status.getSeverity() == IStatus.OK) {
                    setErrorMessage(null);
                    setOkButtonEnabled(true);
                } else {
                    if (setupFinished) {
                        setErrorMessage(status.getMessage());
                    }
                    setOkButtonEnabled(false);
                }
            }
        });
        dbc.bindValue(statusObservable,
            new AggregateValidationStatus(dbc.getBindings(),
                AggregateValidationStatus.MAX_SEVERITY));
    }

    protected void setOkButtonEnabled(boolean enabled) {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null && !okButton.isDisposed()) {
            okButton.setEnabled(enabled);
        } else {
            okButtonEnabled = enabled;
        }
    }

    @Override
    protected void okPressed() {
        try {
            new URL(DEFAULT_UNSECURE_PREFIX + serverWidget.getText());
        } catch (MalformedURLException e) {
            MessageDialog.openError(getShell(),
                Messages.getString("LoginDialog.field.server.url.error.title"), //$NON-NLS-1$
                Messages.getString("LoginDialog.field.server.url.error.msg")); //$NON-NLS-1$
            return;
        }

        if (!BiobankPlugin.getDefault().isDebugging()) {
            // until further notice, we still want to be able to specify the
            // port, even in non debug mode
            // if (url.getPort() != -1) {
            // MessageDialog
            // .openError(getShell(), "Invalid Server URL",
            // "You are not allowed to specify a port, only a hostname and path.");
            // return;
            // }
            if (userNameWidget.getText().equals("")) { //$NON-NLS-1$
                MessageDialog.openError(getShell(),
                    Messages.getString("LoginDialog.field.user.error.title"), //$NON-NLS-1$
                    Messages.getString("LoginDialog.field.user.error.msg")); //$NON-NLS-1$
                return;
            }
        }

        boolean secureConnection = ((secureConnectionButton == null) || secureConnectionButton
            .getSelection());

        SessionHelper sessionHelper = new SessionHelper(serverWidget.getText(),
            secureConnection, userNameWidget.getText(),
            passwordWidget.getText());

        BusyIndicator.showWhile(PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell().getDisplay(), sessionHelper);

        if (sessionHelper.getUser() != null) {
            if (superAdminWidget.getSelection()) {
                // super admin mode
                superAdminMode = sessionHelper.getUser().isSuperAdministrator();
                if (!superAdminMode)
                    BiobankPlugin
                        .openAsyncError("Super administrator",
                            "You don't have rights to connect as super administrator");
            }
            if (!superAdminMode) {
                selectWorkingCenter(sessionHelper);
            }
            if (superAdminMode
                || sessionHelper.getUser().getCurrentWorkingCenter() != null) {
                // login successful
                savePreferences();
                SessionManager.getInstance().addSession(
                    sessionHelper.getAppService(), serverWidget.getText(),
                    sessionHelper.getUser(), superAdminMode);
            }

        }
        super.okPressed();
    }

    private void savePreferences() {
        pluginPrefs.put(LAST_SERVER, serverWidget.getText());
        pluginPrefs.put(LAST_USER_NAME, userNameWidget.getText());

        if ((serverWidget.getText().length() > 0)
            && (serverWidget.getSelectionIndex() == -1)
            && !servers.contains(serverWidget.getText())) {
            IPreferenceStore prefsStore = BiobankPlugin.getDefault()
                .getPreferenceStore();
            StringBuilder serverList = new StringBuilder();
            for (String server : servers) {
                serverList.append(server);
                serverList.append("\n"); //$NON-NLS-1$
            }
            prefsStore.putValue(PreferenceConstants.SERVER_LIST, serverList
                .append(serverWidget.getText().trim()).toString());
        }

        if ((userNameWidget.getText().length() > 0)
            && (userNameWidget.getSelectionIndex() == -1)
            && !userNames.contains(userNameWidget.getText())) {
            Preferences prefsUserNames = pluginPrefs.node(SAVED_USER_NAMES);
            Preferences prefsUserName = prefsUserNames.node(Integer
                .toString(userNames.size()));
            prefsUserName.put(USER_NAME, userNameWidget.getText().trim());
        }

        try {
            pluginPrefs.flush();
        } catch (BackingStoreException e) {
            logger.error("Could not save loggin preferences", e); //$NON-NLS-1$
        }
    }

    private void selectWorkingCenter(SessionHelper sessionHelper) {
        List<CenterWrapper<?>> workingCenters = null;
        try {
            workingCenters = sessionHelper.getUser().getWorkingCenters(
                sessionHelper.getAppService());
        } catch (Exception e) {
            BiobankPlugin
                .openAsyncError(Messages
                    .getString("LoginDialog.working.center.error.title"), e); //$NON-NLS-1$
        }
        if (workingCenters != null) {
            if ((workingCenters.size() == 0)
                && !sessionHelper.getUser().isSuperAdministrator())
                // cannot access the application.
                BiobankPlugin.openAsyncError(Messages
                    .getString("LoginDialog.working.center.error.title"), //$NON-NLS-1$
                    Messages
                        .getString("LoginDialog.no.working.center.error.msg")); //$NON-NLS-1$
            else if (workingCenters.size() == 1) {
                sessionHelper.getUser().setCurrentWorkingCenter(
                    workingCenters.get(0));
            } else if (workingCenters.size() > 1) {
                new WorkingCenterSelectDialog(getShell(),
                    sessionHelper.getAppService(), sessionHelper.getUser())
                    .open();
            }
        }
        if (sessionHelper.getUser().getCurrentWorkingCenter() == null)
            if (sessionHelper.getUser().isSuperAdministrator()) {
                // connect in admin mode
                BiobankPlugin.openAsyncInformation(Messages
                    .getString("LoginDialog.working.center.admin.title"), //$NON-NLS-1$
                    Messages
                        .getString("LoginDialog.no.working.center.admin.msg")); //$NON-NLS-1$
                // open the administration perspective if another
                // perspective is open
                superAdminMode = true;
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow activeWindow = workbench
                    .getActiveWorkbenchWindow();
                IWorkbenchPage page = activeWindow.getActivePage();
                if (!page.getPerspective().getId().equals(MainPerspective.ID)) {
                    try {
                        workbench.showPerspective(MainPerspective.ID,
                            activeWindow);
                    } catch (WorkbenchException e) {
                        BiobankPlugin
                            .openAsyncError(
                                Messages
                                    .getString("LoginDialog.perspective.open.error.msg"), //$NON-NLS-1$
                                e);
                    }
                }
            } else {
                BiobankPlugin
                    .openAsyncError(
                        Messages
                            .getString("LoginDialog.working.center.selection.error.title"), //$NON-NLS-1$
                        Messages
                            .getString("LoginDialog.working.center.selection.error.msg")); //$NON-NLS-1$
            }
    }

    public class Authentication {
        public static final String SERVER_PROPERTY_NAME = "server"; //$NON-NLS-1$
        public static final String USERNAME_PROPERTY_NAME = "username"; //$NON-NLS-1$
        public static final String PASSWORD_PROPERTY_NAME = "password"; //$NON-NLS-1$

        public String server;
        public String username;
        public String password;

        public void setServer(String server) {
            this.server = server;
        }

        public String getServer() {
            return server;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            return server + "/" + username + "/" + password; //$NON-NLS-1$ //$NON-NLS-2$ 
        }
    }
}
