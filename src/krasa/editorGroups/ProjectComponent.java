package krasa.editorGroups;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "EditorGroups", storages = {@Storage(value = "EditorGroups.xml")})
public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent, PersistentStateComponent<ProjectComponent.State> {
	private static final Logger LOG = Logger.getInstance(ProjectComponent.class);

	private final Project project;

	public ProjectComponent(Project project) {
		this.project = project;
	}

	@Override
	public void projectOpened() {
		EditorGroupManager.getInstance(project).initCache();

		FileEditorManagerListener.Before before = new FileEditorManagerListener.Before() {

			@Override
			public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
				LOG.debug("beforeFileClosed [" + file + "]");
			}
		};
		project.getMessageBus().connect().subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, before);	
			
		project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {

			//IJ 2018.2
			@Override
			public void fileOpenedSync(@NotNull FileEditorManager manager, @NotNull VirtualFile file, @NotNull Pair<FileEditor[], FileEditorProvider[]> editors) {
				LOG.debug("fileOpenedSync [" + file + "], editors = [" + editors + "]");

				EditorGroupManager instance = EditorGroupManager.getInstance(project);
				EditorGroup switchingGroup = instance.getSwitchingGroup(file);
				int myScrollOffset = instance.myScrollOffset;

				for (FileEditor fileEditor : editors.getFirst()) {
					if (fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL) != null) {
						continue;
					}
					long start = System.currentTimeMillis();
					instance.myScrollOffset = 0;
					EditorGroupPanel panel = new EditorGroupPanel(fileEditor, project, switchingGroup, file, myScrollOffset);

					manager.addTopComponent(fileEditor, panel.getRoot());
					if (LOG.isDebugEnabled()) {
						LOG.debug("sync EditorGroupPanel created, file=" + file + " in " + (System.currentTimeMillis() - start) + "ms" + ", fileEditor=" + fileEditor);
					}
				}


			}

			/**on EDT*/
			@Override
			public void fileOpened(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
				LOG.debug("fileOpened [" + file + "]");
				final FileEditor[] fileEditors = manager.getAllEditors(file);

				EditorGroupManager instance = EditorGroupManager.getInstance(project);
				EditorGroup switchingGroup = instance.getSwitchingGroup(file);
				int myScrollOffset = instance.myScrollOffset;

				for (final FileEditor fileEditor : fileEditors) {
					if (fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL) != null) {
						continue;
					}
					long start = System.currentTimeMillis();

					instance.myScrollOffset = 0;
					EditorGroupPanel panel = new EditorGroupPanel(fileEditor, project, switchingGroup, file, myScrollOffset);

					manager.addTopComponent(fileEditor, panel.getRoot());
					if (LOG.isDebugEnabled()) {
						LOG.debug("async EditorGroupPanel created, file=" + file + " in " + (System.currentTimeMillis() - start) + "ms" + ", fileEditor=" + fileEditor);
					}
				}

			}

			@Override
			public void selectionChanged(@NotNull FileEditorManagerEvent event) {
				FileEditor fileEditor = event.getNewEditor();
				if (fileEditor instanceof TextEditorImpl) {
					return; //focus listener handles that
				}
				if (fileEditor != null) {
					EditorGroupPanel panel = fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
					if (panel != null) {    //UI form editor is not disposed, so the panel might exist and it has no focus listener... 
						EditorGroup switchingGroup = EditorGroupManager.getInstance(project).getSwitchingGroup(panel.getFile());
						LOG.debug("selectionChanged, refresh");
						panel.refresh(false, switchingGroup);
					}
				}
			}

			@Override
			public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
				LOG.debug("fileClosed [" + file + "]");
			}
		});
	}

	@Nullable
	@Override
	public State getState() {
		long start = System.currentTimeMillis();
		State state = IndexCache.getInstance(project).getState();
		LOG.debug("ProjectComponent.getState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
		return state;
	}

	@Override
	public void loadState(@NotNull State state) {
		long start = System.currentTimeMillis();
		IndexCache.getInstance(project).loadState(state);
		LOG.debug("ProjectComponent.loadState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
	}

	public static class State {
		@XCollection(propertyElementName = "lastGroup", elementTypes = StringPair.class)
		public List<StringPair> lastGroup = new ArrayList<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			State state = (State) o;

			return lastGroup != null ? lastGroup.equals(state.lastGroup) : state.lastGroup == null;
		}

		@Override
		public int hashCode() {
			return lastGroup != null ? lastGroup.hashCode() : 0;
		}
	}


	@Tag("p")
	public static class StringPair {
		@Attribute("k")
		public String key;
		@Attribute("v")
		public String value;

		public StringPair() {
		}

		public StringPair(String key, String value) {
			this.key = key;
			this.value = value;
		}


	}


}
