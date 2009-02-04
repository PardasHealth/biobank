package edu.ualberta.med.biobank.session;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import edu.ualberta.med.biobank.model.RootNode;
import edu.ualberta.med.biobank.model.SessionNode;
import edu.ualberta.med.biobank.model.SiteNode;
import edu.ualberta.med.biobank.model.ClinicGroupNode;
import edu.ualberta.med.biobank.model.StudyGroupNode;

public class SessionContentProvider implements ITreeContentProvider {
	
	@SuppressWarnings("unused")
	private RootNode rootNode;
	
	public SessionContentProvider() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object element) {
		if (element instanceof RootNode) {
			return ((RootNode) element).getSessions();
		}
		else if (element instanceof SessionNode) {
			return ((SessionNode) element).getSites();
		}
		else if (element instanceof SiteNode) {
			return ((SiteNode) element).getChildren();
		}
		else if (element instanceof ClinicGroupNode) {
			return ((ClinicGroupNode) element).getClinicNodes();
		}
		else if (element instanceof StudyGroupNode) {
			return ((StudyGroupNode) element).getStudieNodes();
		}
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof RootNode) {
			return null;
		}
		else if (element instanceof SessionNode) {
			return ((SessionNode) element).getParent();
		}
		else if (element instanceof SiteNode) {
			return ((SiteNode) element).getParent();
		}
		else if (element instanceof ClinicGroupNode) {
			return ((ClinicGroupNode) element).getParent();
		}
		else if (element instanceof StudyGroupNode) {
			return ((StudyGroupNode) element).getParent();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object element) {
		return getChildren(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof RootNode)
			rootNode = (RootNode) newInput;
		else if (newInput == null)
			rootNode = null;
	}
}
