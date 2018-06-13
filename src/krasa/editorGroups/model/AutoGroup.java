package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class AutoGroup extends EditorGroup {

	public static final String SAME_FILE_NAME = "FILE_NAME";
	public static final String DIRECTORY = "DIRECTORY";

	public static final FolderGroup DIRECTORY_INSTANCE = new FolderGroup("DIRECTORY_INSTANCE", Collections.emptyList());
	public static final SameNameGroup SAME_NAME_INSTANCE = new SameNameGroup("SAME_NAME_INSTANCE", ContainerUtilRt.emptyList());


	protected final List<String> links;
	protected volatile boolean valid = true;

	public AutoGroup(List<String> links) {
		this.links = links;
	}


	@NotNull
	@Override
	public abstract String getId();


	@Override
	public abstract String getTitle();

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public void invalidate() {
		valid = false;

	}

	@Override
	public int size(Project project) {
		return links.size();
	}

	@Override
	public List<String> getLinks(Project project) {
		return links;
	}

	@Override
	public boolean isOwner(String ownerPath) {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return obj.getClass().equals(this.getClass());
	}


	@Override
	public boolean equalsVisually(Project project, EditorGroup group) {
		if (!super.equalsVisually(project, group)) {
			return false;
		}

		return true;
	}

	public boolean isEmpty() {
		return links.size() == 0;
	}

	;
	
}
