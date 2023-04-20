package io.exonym.utils.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.exceptions.ExceptionCollection;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.XKey;

public class NetworkContainer {
	
	private final URI networkUid; 
	private final String path = "resource//network";
	
	private final String categories = "categories";
	private final String chats = "chats";
	private final String facades = "facades";
	private final String groups = "groups";
	private final String peers = "peers";
	private final String indices = "indices";
	private final String sharedItems = "shared-items";
	private final String openNetworkQueries = "open-network-queries";
	private final ConcurrentHashMap<String, File> folders = new ConcurrentHashMap<>();  
	
	private final File root;
	
	public void addPeer(XPeer peer) throws Exception{
		String myNym = peer.getMyNym();
		String myScope = peer.getMyScope();
		String theirNym = peer.getTheirNym();
		String theirScope = peer.getTheirScope();
		String username = peer.getContainerName();
		XKey myKey = peer.getMyKey();
		XKey theirKey = peer.getTheirKey();
		
		ExceptionCollection c = new ExceptionCollection();
		Exception base = new Exception("A new Peer must have the following fields set to valid values;");
		
		boolean failed = false; 
		if (myNym==null){
			failed = addMessage("\tMy Pseudoym", c, base, failed);
			
		} if (myScope == null){
			failed = addMessage("\tMy Scope", c, base, failed);
			
		} if (theirNym == null){
			failed = addMessage("\tTheir Pseudoym", c, base, failed);
			
		} if (username == null){
			failed = addMessage("\tUsername", c, base, failed);
			
		} if (theirScope == null){
			failed = addMessage("\tTheir Scope", c, base, failed);
			
		} 
		if (myKey == null){
			failed = addMessage("\tMy Key", c, base, failed);
			
		} else {
			if (myKey.getPrivateKey()==null){
				failed = addMessage("\tMy Private Key", c, base, failed);
				
			} if (myKey.getPublicKey()==null){
				failed = addMessage("\tMy Public Key", c, base, failed);
				
			} 
		}
		if (theirKey == null){ 
			failed = addMessage("\tTheir Key", c, base, failed);
			
		} else if (theirKey.getPublicKey()==null) {
			failed = addMessage("\tTheir Public Key", c, base, failed);
				
		}		
		if (c.isEmpty()){
			File folder = folder(peers);
			String fn = theirNym + ".xml";
			File f = new File(folder.getPath() +"\\" + fn);
			if (!f.exists()){
				String xml = JaxbHelper.serializeToXml(peer, XPeer.class);
				save(xml, f, false);
				addQuickEndonym(theirNym);
				
			} else {
				throw new Exception("Peer already exists - use Update Peer");
				
			}
		} else {
			throw c; 
			
		}
	}
	
	public void updatePeer(XPeer peer) throws Exception{
		XPeer former = openPeer(peer.getTheirNym());
		Exception e = new Exception("Unable to update scope or pseudonym.  "
				+ "Add a new peer or make a new connection.");
		
		if (!peer.getTheirScope().equals(former.getTheirScope())){
			throw e;
			
		} if (!peer.getMyNym().equals(former.getMyNym())){
			throw e;
			
		} if (!peer.getContainerName().equals(former.getContainerName())){
			throw e;
			
		} if (!peer.getMyScope().equals(former.getMyScope())){
			throw e;
			
		}
		File folder = folder(peers);
		String fn = peer.getTheirNym() + ".xml";
		File f = new File(folder.getPath() +"\\" + fn);
		String xml = JaxbHelper.serializeToXml(peer, XPeer.class);
		if (f.exists()){
			save(xml, f, true);
			
		} else {
			throw new Exception("Peer does not exist - use Add Peer");
			
		}
	}

	private boolean addMessage(String failureItem, ExceptionCollection c, Exception base, boolean failed) {
		if (!failed){
			c.addException(base);
			
		}
		c.addException(new Exception(failureItem));
		return true;

	}

	public void addGroup(Group group) throws Exception {
		URI uid = group.getGroupUid();
		String name = group.getName();
		
		ExceptionCollection c = new ExceptionCollection();
		if (uid==null){
			c.addException(new HubException("Define a GroupUID for this group"));
			
		} if (name==null){ 
			c.addException(new UxException("You must define a group name for this group"));

		} 
		if (c.isEmpty()){
			addCategoriesToGroup(group);
			String xml = JaxbHelper.serializeToXml(group, Group.class);
			String fn = XContainer.uidToXmlFileName(uid);
			File groupFolder = folder(groups);
			File f = new File(groupFolder.getPath() + "\\" + fn);
			save(xml, f, false);
			
		} else {
			throw c; 
			
		}
	}
	
	public void addUpdateNetworkQuery(OpenNetworkQuery onq) throws Exception {
		URI uid = onq.getQueryUid(); 
		String username = onq.getUsername();
		XPeer endonym = onq.getTargetPeer();
		XNodeMsg messages = onq.getOutboundMessage();
		URI ptUid = onq.getPresentationTokenUid();
		boolean content = (messages!=null || ptUid!=null);
		ExceptionCollection ec = new ExceptionCollection();
		
		if (uid==null){
			ec.addException(new Exception("OpenNetworkQueryUID"));
			
		} if (username==null){
			ec.addException(new Exception("Username of sender"));
			
		} if (endonym==null){
			ec.addException(new Exception("Endonym of peer"));
			
		} if(!content) {
			ec.addException(new Exception("No content"));
			
		}
		if (ec.isEmpty()){
			File folder = folder(openNetworkQueries);
			String fn = XContainer.uidToXmlFileName(onq.getQueryUid());
			File f = new File(folder.getPath() +"\\" + fn);
			String xml = JaxbHelper.serializeToXml(onq, OpenNetworkQuery.class);
			save(xml, f, true);
			
		} else {
			throw ec;
			
		}
	}
	
	public ConfirmationIndex openConfirmationIndex() throws Exception{
		File folder = folder(indices);
		File file = new File(folder.getPath() +"//" + "confirmation.xml");
		if (file.exists()){
			return JaxbHelper.xmlFileToClass(
					JaxbHelper.fileToPath(file), ConfirmationIndex.class);
			
		} else {
			return new ConfirmationIndex();
			
		}
	}
	
	public void addConfirmationIndex(ConfirmationIndex i) throws Exception{
		File folder = folder(indices);
		File file = new File(folder.getPath() +"//" + "confirmation.xml");
		String xml = JaxbHelper.serializeToXml(i, ConfirmationIndex.class);
		save(xml, file, true);
		
	}
	
	public void commitGroup(Group group) throws Exception {
		URI uid = group.getGroupUid();
		String name = group.getName();
		
		if (uid==null){
			throw new UxException("The UID must be set");
			
		} if (name==null){
			throw new UxException("A group name must be set");
			
		}
		File folder = folder(groups);
		String fn = XContainer.uidToXmlFileName(uid);
		File f = new File(folder.getPath() +"\\" + fn);
		
		if (f.exists()){
			String xml = JaxbHelper.serializeToXml(group, Group.class);
			save(xml, f, true);
			
		} else {
			throw new HubException("Use add group to create a new group");
			
		}
	}

	private void addCategoriesToGroup(Group group) throws Exception {
		File catFolder = folder(categories);
		File[] files = catFolder.listFiles();
		ArrayList<Category> cs = group.getCategories();
		
		for (int i = 0; i < files.length; i++) {
			Category c = JaxbHelper.xmlFileToClass(
					JaxbHelper.fileToPath(files[i]), Category.class);
			cs.add(c);
			
		}
	}
	
	/**
	 * <b>Warning : O(n^2*n^3)</b> <p>
	 * 
	 * There should be relatively few categories; but the number is not limited.
	 * 
	 * This could take a while to run if n was to get large.
	 * 
	 * @throws Exception
	 */
	public void refreshCategories() throws Exception{
		File folder = folder(groups);
		File[] files = folder.listFiles();

		for (int i = 0; i < files.length; i++) {
			ArrayList<String> catNameFalseFlags = new ArrayList<>();
			Group g = JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(files[i]), Group.class);
			
			for (Category c : g.getCategories()){
				
				for (SubCategory s : c.getSubCategories()){
					
					if (!s.isAllowed()){
						 catNameFalseFlags.add(catSubCat(c, s));
						 
					}
				}
			}
			g.getCategories().clear();
			addCategoriesToGroup(g);
			for (Category c : g.getCategories()){
				
				for (SubCategory s : c.getSubCategories()){
					
					String csc = catSubCat(c, s);
					
					if (catNameFalseFlags.contains(csc)){
						s.setAllowed(false);
						
					}
				}
			}
			String xml = JaxbHelper.serializeToXml(g, Group.class);
			String fn = XContainer.uidToXmlFileName(g.getGroupUid());
			File groupFolder = folder(groups);
			File f = new File(groupFolder.getPath() + "\\" + fn);
			save(xml, f, true);

		}
	}
	
	public void addUpdateFacade(NetworkFacade facade) throws Exception{
		URI uid = facade.getFacadeUid();
		String name = facade.getScreenName();
		ExceptionCollection c = new ExceptionCollection();
		if (uid==null){
			c.addException(new HubException("Define a FacadeUID for this facade."));
			
		} if (name==null){ 
			c.addException(new UxException("You must define a Screen Name for this Facade."));
			
		}
		if (c.isEmpty()){
			File folder = folder(facades);
			String fn = XContainer.uidToXmlFileName(uid);
			File f = new File(folder.getPath() +"\\" + fn);
			String xml = JaxbHelper.serializeToXml(facade, NetworkFacade.class);
			save(xml, f, true);
			
		} else {
			throw c; 
			
		}
	}
	
	private String catSubCat(Category c, SubCategory s){
		return c.getCategoryUid() + ":" + s.getName();
		
	}
	
	public Group openGroup(URI groupUid) throws Exception{
		File folder = folder(groups);
		String fn = XContainer.uidToXmlFileName(groupUid);
		File f = new File(folder.getPath() +"\\" + fn);
		if (f.exists()){
			return JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(f), Group.class);
			
		} else {
			throw new UxException("The group " + groupUid + " does not exist");
			
		}
	}
	
	
	public Category openCategory(URI uid) throws Exception{
		File folder = folder(categories);
		String fn = XContainer.uidToXmlFileName(uid);
		File f = new File(folder.getPath() +"\\" + fn);
		if (f.exists()){
			return JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(f), Category.class);
			
		} else {
			throw new UxException("The category " + uid + " does not exist");
			
		}
	}
	
	public NetworkFacade openFacade(URI uid) throws Exception{
		File folder = folder(facades);
		String fn = XContainer.uidToXmlFileName(uid);
		File f = new File(folder.getPath() +"\\" + fn);
		if (f.exists()){
			return JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(f), NetworkFacade.class);
			
		} else {
			throw new UxException("The facade " + uid + " does not exist");
			
		}
	}		
	
	public XPeer openPeer(String nym) throws Exception{
		if (nym==null){
			throw new Exception("You must provided a pseudonym");
			
		} else {
			File folder = folder(peers);
			String fn = nym + ".xml";
			File f = new File(folder.getPath() +"\\" + fn);
			if (f.exists()){
				return JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(f), XPeer.class);
				
			} else {
				throw new UxException("The Peer with " + nym + " does not exist");
				
			}
		}
	}
	
	public XPeer[] openPeers() throws Exception {
		File folder = folder(peers);
		File[] files = folder.listFiles();
		XPeer[] result = new XPeer[files.length];
		
		for (int i = 0; i < files.length; i++) {
			result[i] = JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(files[i]), XPeer.class);
			
		}
		return result;
		
	}
	
	public OpenNetworkQuery[] openNetworkQueries() throws Exception{
		return openNetworkQueries(false);
		
	}
	
	public OpenNetworkQuery[] openNetworkQueries(boolean delete) throws Exception{
		File folder = folder(openNetworkQueries);
		File[] files = folder.listFiles();
		if (files!=null){
			OpenNetworkQuery[] result = new OpenNetworkQuery[files.length];
			
			for (int i = 0; i < files.length; i++) {
				result[i] = JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(files[i]), OpenNetworkQuery.class);
				if (delete){
					files[i].delete();	
					
				}
			}
			return result;
			
		} else {
			return null; 
			
		}
	}
	
	public QuickEndonym openQuickEndonym() throws Exception{
		File folder = folder(indices);
		File file = new File(folder.getPath() + "\\endonyms.xml");
		if (file.exists()){
			return JaxbHelper.xmlFileToClass(JaxbHelper.fileToPath(file), QuickEndonym.class);
			
		} else {
			return new QuickEndonym();
			
		}
	}
	
	public QuickEndonym addQuickEndonym(String nym) throws Exception{
		QuickEndonym qe = openQuickEndonym();
		qe.getEndonyms().add(nym);
		File folder = folder(indices);
		File file = new File(folder.getPath() + "\\endonyms.xml");
		save(JaxbHelper.serializeToXml(qe, QuickEndonym.class), file, true);
		return qe; 
		
	}

	private File folder(String items) {
		if (folders.isEmpty()){
			arrayOfFolders();
			
		} 
		return folders.get(items);
		
	}
	
	public void deleteCategory(URI categoryUid) throws Exception{
		File folder = folder(categories);
		String fn = XContainer.uidToXmlFileName(categoryUid);
		File f = new File(folder.getPath() +"\\" + fn);
		
		if (f.exists()){
			f.delete();
			
		}
	}
	
	public void deleteNetwork(){
		if (root.exists()){
			File[] folders = arrayOfFolders();
			
			for (int i = 0; i < folders.length; i++) {
				File[] files = folders[i].listFiles();
				if (files!=null){
					for (int j = 0; j < files.length; j++) {
						files[j].delete();
						
					
					}
				}
				folders[i].delete();
				
			}
			root.delete();
		}
	}	
	
	public void deleteGroup(URI groupUid) throws Exception{
		File folder = folder(groups);
		String fn = XContainer.uidToXmlFileName(groupUid);
		File f = new File(folder.getPath() +"\\" + fn);
		
		if (f.exists()){
			f.delete();
			
		}
	}

	public void addUpdateCategory(Category cat) throws Exception{
		URI uid = cat.getCategoryUid();
		String name = cat.getName();
		TreeSet<SubCategory> subs = cat.getSubCategories();
		ExceptionCollection c = new ExceptionCollection();
		if (uid==null){
			c.addException(new HubException("Define a UID for this category"));
			
		} if (name==null){ 
			c.addException(new UxException("Add a name for this category."));
			
		} if (subs.isEmpty()){
			c.addException(new UxException("Add at least one category."));
			
		}
		if (c.isEmpty()){
			File folder = folder(categories);
			String fn = XContainer.uidToXmlFileName(uid);
			File f = new File(folder.getPath() +"\\" + fn);
			String xml = JaxbHelper.serializeToXml(cat, Category.class);
			save(xml, f, true);
			
		} else {
			throw c; 
			
		}
	}

	public NetworkContainer(URI networkUid) throws Exception {
		this.networkUid=networkUid;
		root = generateRoot(networkUid);
		if (!root.exists()){
			throw new Exception("Bad Network UID.  To create a network uid, you must set the create flag to true.");
		
		}
	}

	public NetworkContainer(URI networkUid, boolean create) throws Exception {
		this.networkUid=networkUid;
		root = generateRoot(networkUid);
		if (!root.exists()){
			makeFolders();
			addCategories();
			createGroups();
			
		} else {
			throw new Exception("NetworkUID already exists " + networkUid);
			
		}
	}
	
	private void addCategories() throws Exception {
		String[] names = new String[]{
			"Status Updates",
			"Photo Albums"
		};
		for (int i = 0; i < names.length; i++) {
			Category c = new Category();
			c.setName(names[i]);
			c.setCategoryUid(nameToUid(names[i]));
			if (i==0){
				SubCategory sc0 = new SubCategory();
				sc0.setName("Personal");
				sc0.setAllowed(false);
				SubCategory sc1 = new SubCategory();
				sc1.setName("Just thinking...");
				SubCategory sc2 = new SubCategory();
				sc2.setName("Rant");
				sc2.setAllowed(false); // on create of a new group, the default for "Rants" be false.
				// I.e. people in that group needs to be explicitly allowed access to the user's rants.
				
				TreeSet<SubCategory> t = new TreeSet<>();
				t.add(sc0);
				t.add(sc1);
				t.add(sc2);
				c.setSubCategories(t);
				
			} else if (i==1){
				SubCategory sc0 = new SubCategory();
				sc0.setName("Family");
				sc0.setAllowed(false);
				SubCategory sc1 = new SubCategory();
				sc1.setName("Friends");
				sc0.setAllowed(false);
				SubCategory sc2 = new SubCategory();
				sc2.setName("Nights Out");
				sc2.setAllowed(false);
				
				TreeSet<SubCategory> t = new TreeSet<>();
				t.add(sc0);
				t.add(sc1);
				t.add(sc2);
				c.setSubCategories(t);
				
			}
			
			this.addUpdateCategory(c);
			
		}
	}

	private void createGroups() throws Exception {
		String[] groupNames = new String[] {
				"Friends",
				"Family",
				"New Contacts",
				"Transactional Peers",
				"Issuance",
				"Service Authentication",
				
		};
		Group[] groups = new Group[groupNames.length];
		for (int i = 0; i < groupNames.length; i++) {
			Group g = new Group();
			g.setGroupUid(nameToUid(groupNames[i]));
			g.setName(groupNames[i]);
			groups[i] = g;
			
		}
		for (int i = 0; i < groups.length; i++) {
			this.addGroup(groups[i]);
			
		}
	}

	private URI nameToUid(String n) throws Exception {
		if (n==null){
			throw new Exception("Programming error");
			
		}
		return URI.create("urn:x:network:" + n.toLowerCase().replaceAll(" ", "-"));

	}

	private File generateRoot(URI networkUid) throws Exception {
		return new File(path + "//" + XContainer.uidToFileName(networkUid));
		
	}
	
	private void makeFolders() {
		root.mkdirs();
		File[] folders = arrayOfFolders();
		for (int i = 0; i < folders.length; i++) {
			folders[i].mkdir();
			
		}
	}

	private File[] arrayOfFolders() {
		File[] folders = new File[]{
				new File(root.getPath() + "//" + groups),
				new File(root.getPath() + "//" + categories),
				new File(root.getPath() + "//" + indices),
				new File(root.getPath() + "//" + peers),
				new File(root.getPath() + "//" + facades),
				new File(root.getPath() + "//" + chats),
				new File(root.getPath() + "//" + sharedItems),
				new File(root.getPath() + "//" + openNetworkQueries),
		
		};
		if (this.folders.isEmpty()){
			this.folders.put(groups, folders[0]);
			this.folders.put(categories, folders[1]);
			this.folders.put(indices, folders[2]);
			this.folders.put(peers, folders[3]);
			this.folders.put(facades, folders[4]);
			this.folders.put(chats, folders[5]);
			this.folders.put(sharedItems, folders[6]);
			this.folders.put(openNetworkQueries, folders[7]);
			
		}
		return folders;
		
	}
	
	private void save(String xml, File f, boolean overwrite) throws Exception {
		if (overwrite && f.exists()){
			f.delete();
			
		}
		if (!f.exists()){
			try (FileOutputStream fos = new FileOutputStream(f)){
				fos.write(xml.getBytes());
				
			} catch (Exception e) {
				throw e;
				
			}
		} else {
			throw new UxException("A file with this name already exists.");
			
		}
	}	

	public URI getNetworkUid() {
		return networkUid;
		
	}
	
	public File getRoot() {
		return root;
	}
}
