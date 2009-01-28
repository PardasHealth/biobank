package edu.ualberta.med.biobank.session;

import java.util.HashMap;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import edu.ualberta.med.biobank.Activator;
import edu.ualberta.med.biobank.SessionCredentials;
import edu.ualberta.med.biobank.model.Address;
import edu.ualberta.med.biobank.model.RootNode;
import edu.ualberta.med.biobank.model.SiteNode;
import edu.ualberta.med.biobank.model.SessionNode;
import edu.ualberta.med.biobank.model.ISessionNodeListener;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.client.ApplicationServiceProvider;
import gov.nih.nci.system.query.SDKQuery;
import gov.nih.nci.system.query.SDKQueryResult;
import gov.nih.nci.system.query.example.InsertExampleQuery;
import edu.ualberta.med.biobank.model.Site;

public class SessionsView extends ViewPart implements ISelectionChangedListener {
	public static final String ID =
	      "edu.ualberta.med.biobank.session.SessionView";

	private TreeViewer treeViewer;
	
	private RootNode rootNode;
	
	private HashMap<String, SessionNode> sessions;
	
	public SessionsView() {
		super();
		Activator.getDefault().setSessionView(this);
		rootNode = new RootNode();
		sessions = new  HashMap<String, SessionNode>();
	}

	@Override
	public void createPartControl(Composite parent) {
		
		treeViewer = new TreeViewer(parent, SWT.BORDER | SWT.MULTI
				| SWT.V_SCROLL);
		getSite().setSelectionProvider(treeViewer);
		treeViewer.setLabelProvider(new SessionLabelProvider());
		treeViewer.setContentProvider(new SessionContentProvider());
        treeViewer.addSelectionChangedListener(this);
	}
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
	public void loginFailed(SessionCredentials sc) {
		// pop up a dialog box here
		
	}
	
	public void createSession(final SessionCredentials sc) {
		Job job = new Job("logging in") {
			protected IStatus run(IProgressMonitor monitor) {
				
				monitor.beginTask("Logging in ... ", 100);					
				try {
					final WritableApplicationService appService;
					final String userName = sc.getUserName(); 
					final String url = "http://" + sc.getServer() + "/biobank2";
					
					if (userName.length() == 0) {
						appService =  (WritableApplicationService) 
						ApplicationServiceProvider.getApplicationServiceFromUrl(url);
					}
					else {
						appService = (WritableApplicationService) 
						ApplicationServiceProvider.getApplicationServiceFromUrl(url, userName, sc.getPassword());
					}

					
					Display.getDefault().asyncExec(new Runnable() {
				          public void run() {
				        	  Activator.getDefault().addSession(appService, sc.getServer());
				          }
					});
				}
				catch (Exception exp) {	
					exp.printStackTrace();
					
					Display.getDefault().asyncExec(new Runnable() {
				          public void run() {
				        	  Activator.getDefault().addSessionFailed(sc);
				          }
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
	
	public void addSession(final WritableApplicationService appService, final String name)  throws Exception {
		final SessionNode sessionNode = new SessionNode(appService, name);
		sessions.put(name, sessionNode);
		rootNode.addSessionNode(sessionNode);
		
		treeViewer.setInput(rootNode);
		sessionNode.addListener(new ISessionNodeListener() {
			public void sessionChanged(SessionNode sessionNode, SiteNode siteNode) {
				treeViewer.refresh();
			}
		});
		
		updateSites(name);
	}
	
	public void updateSites(final String sessionName) throws Exception {
		if (!sessions.containsKey(sessionName)) {
			throw new Exception();
		}
		
		final SessionNode sessionNode = sessions.get(sessionName);
		
		// get the Site sites stored on this server
		Job job = new Job("logging in") {
			protected IStatus run(IProgressMonitor monitor) {
				
				monitor.beginTask("Querying Sites ... ", 100);
				
				Site site = new Site();				
				try {
					WritableApplicationService appService = sessionNode.getAppService();
					final List<Object> sites = appService.search(Site.class, site);
					
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							for (Object obj : sites) {
								Site site = (Site) obj;
								sessionNode.addSite(site);
							}
							treeViewer.expandToLevel(2);
						}
					});
				}
				catch (Exception exp) {
					exp.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
	
	public void createObject(final String sessionName, final Object o) throws Exception {
		if (!sessions.containsKey(sessionName)) {
			throw new Exception();
		}
		
		final SessionNode sessionNode = sessions.get(sessionName);
		
		Job job = new Job("Creating Object") {
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Submitting information ... ", 100);
				
				try {
					SDKQuery query;
					SDKQueryResult result;
					WritableApplicationService appService = sessionNode.getAppService();
					
					if (o instanceof Site) {
						Site site = (Site) o;
						query = new InsertExampleQuery(site.getAddress());					
						result = appService.executeQuery(query);
						site.setAddress((Address) result.getObjectResult());
						query = new InsertExampleQuery(site);	
						appService.executeQuery(query);
					}
					
					updateSites(sessionName);
				}
				catch (Exception exp) {
					exp.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
	
	public void deleteSession(String name) throws Exception {
		rootNode.deleteSessionNode(name);
	}
	
	public int getSessionCount() {
		return rootNode.getChildCount();
	}
	
	public String[] getSessionNames() {
		return sessions.keySet().toArray(new String[sessions.size()]);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object selection = event.getSelection();
		
		if (selection == null) return;
		
		Object element = ((StructuredSelection)selection).getFirstElement();

		if (element instanceof SiteNode) {
			SiteNode node = (SiteNode) element;
			String pageId = edu.ualberta.med.biobank.views.SiteView.ID;
			if ((pageId == null) || (pageId.length() == 0)) {
				// TODO: log an error here
				return;
			}
			
			IWorkbenchWindow activeWorkbenchWindow
			= PlatformUI.getWorkbench().getActiveWorkbenchWindow();

			if (activeWorkbenchWindow == null) return;

			IWorkbenchPage page = activeWorkbenchWindow.getActivePage();

			if (page == null) return;

			try {
				page.showView(pageId);
			} 
			catch (PartInitException e) {
				// handle error
				e.printStackTrace();				
			}
			
		}
	}
}
