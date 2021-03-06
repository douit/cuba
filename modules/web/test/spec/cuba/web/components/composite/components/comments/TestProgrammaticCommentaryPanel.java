/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spec.cuba.web.components.composite.components.comments;

import com.google.common.base.Strings;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.ComponentContainer;
import com.haulmont.cuba.gui.components.CssLayout;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.components.data.DataGridItems;
import com.haulmont.cuba.gui.components.data.datagrid.ContainerDataGridItems;
import com.haulmont.cuba.gui.components.data.meta.ContainerDataUnit;
import com.haulmont.cuba.gui.components.sys.ShowInfoAction;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.web.gui.components.CompositeComponent;
import com.haulmont.cuba.web.gui.components.CompositeWithCaption;
import com.haulmont.cuba.web.testmodel.compositecomponent.Comment;

import javax.inject.Inject;
import java.util.function.Function;

public class TestProgrammaticCommentaryPanel extends CompositeComponent<VBoxLayout> implements CompositeWithCaption {

    public static final String NAME = "testProgrammaticCommentaryPanel";

    /* Beans */
    @Inject
    private MetadataTools metadataTools;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Messages messages;

    /* Nested Components */
    private DataGrid<Comment> commentsDataGrid;
    private TextField<String> messageField;
    private Button sendBtn;

    private CollectionContainer<Comment> collectionContainer;
    private Function<String, Comment> commentProvider;

    public TestProgrammaticCommentaryPanel() {
        getEventHub().subscribe(CreateEvent.class, event -> {
            if (getComposition() == null) {
                createComponent();
            }

            initComponent(getCompositionNN());
        });
    }

    private void createComponent() {
        VBoxLayout rootPanel = uiComponents.create(VBoxLayout.class);
        rootPanel.setId("rootPanel");
        rootPanel.setMargin(true);
        rootPanel.setSpacing(true);
        rootPanel.setStyleName("commentary-panel card");
        rootPanel.setWidthFull();

        DataGrid<Comment> commentsDataGrid = uiComponents.create(DataGrid.of(Comment.class));
        commentsDataGrid.setId("commentsDataGrid");
        commentsDataGrid.setBodyRowHeight(100);
        commentsDataGrid.setColumnReorderingAllowed(false);
        commentsDataGrid.setColumnsCollapsingAllowed(false);
        commentsDataGrid.setHeaderVisible(false);
        commentsDataGrid.setSelectionMode(DataGrid.SelectionMode.NONE);
        commentsDataGrid.setWidthFull();

        CssLayout sendMessageBox = uiComponents.create(CssLayout.class);
        sendMessageBox.setId("sendMessageBox");
        sendMessageBox.setStyleName("v-component-group message-box");
        sendMessageBox.setWidthFull();

        TextField<String> messageField = uiComponents.create(TextField.TYPE_STRING);
        messageField.setId("messageField");
        messageField.setInputPrompt("Enter your message");
        messageField.setWidthFull();

        Button sendBtn = uiComponents.create(Button.class);
        sendBtn.setId("sendBtn");
        sendBtn.setCaption(messages.getMessage(TestProgrammaticCommentaryPanel.class, "commentary-panel.send"));

        sendMessageBox.add(messageField, sendBtn);

        rootPanel.add(commentsDataGrid, sendMessageBox);
        rootPanel.expand(commentsDataGrid);

        setComposition(rootPanel);
    }

    @Override
    protected void setComposition(VBoxLayout composition) {
        super.setComposition(composition);

        commentsDataGrid = getInnerComponent("commentsDataGrid");
        messageField = getInnerComponent("messageField");
        sendBtn = getInnerComponent("sendBtn");
    }

    private void initComponent(ComponentContainer composition) {
        commentsDataGrid.addGeneratedColumn("comment", new DataGrid.ColumnGenerator<Comment, String>() {
            @Override
            public String getValue(DataGrid.ColumnGeneratorEvent<Comment> event) {
                Comment item = event.getItem();

                StringBuilder sb = new StringBuilder();
                if (item.getCreatedBy() != null || item.getCreateTs() != null) {
                    sb.append("<p class=\"message-info\">");
                    if (item.getCreatedBy() != null) {
                        sb.append("<span>").append(item.getCreatedBy()).append("</span>");
                    }

                    if (item.getCreateTs() != null) {
                        sb.append("<span style=\"float: right;\">")
                                .append(metadataTools.format(item.getCreateTs()))
                                .append("</span>");
                    }
                    sb.append("</p>");
                }

                sb.append("<p class=\"message-text\">").append(item.getText()).append("</p>");

                return sb.toString();
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        });
        commentsDataGrid.setRowDescriptionProvider(Comment::getText);

        sendBtn.addClickListener(clickEvent ->
                sendMessage());
        messageField.addEnterPressListener(enterPressEvent ->
                sendMessage());
    }

    private void sendMessage() {
        String messageText = messageField.getValue();
        if (!Strings.isNullOrEmpty(messageText)) {
            addMessage(messageText);
            messageField.clear();
        }
    }

    private void addMessage(String text) {
        if (getCommentProvider() == null) {
            return;
        }

        Comment comment = getCommentProvider().apply(text);

        DataGridItems<Comment> items = commentsDataGrid.getItems();
        if (items instanceof ContainerDataUnit) {
            //noinspection unchecked
            CollectionContainer<Comment> container = ((ContainerDataUnit<Comment>) items).getContainer();
            container.getMutableItems().add(comment);
        } else {
            throw new IllegalStateException("Items must implement com.haulmont.cuba.gui.components.data.meta.ContainerDataUnit");
        }
    }

    public Function<String, Comment> getCommentProvider() {
        return commentProvider;
    }

    public void setCreateCommentProvider(Function<String, Comment> commentProvider) {
        this.commentProvider = commentProvider;
    }

    public CollectionContainer<Comment> getCollectionContainer() {
        return collectionContainer;
    }

    public void setDataContainer(CollectionContainer<Comment> container) {
        this.collectionContainer = container;

        commentsDataGrid.setItems(new ContainerDataGridItems<>(container));
        commentsDataGrid.getColumnNN("comment")
                .setRenderer(commentsDataGrid.createRenderer(DataGrid.HtmlRenderer.class));
        commentsDataGrid.removeAction(ShowInfoAction.ACTION_ID);

        container.addCollectionChangeListener(this::onCollectionChanged);
    }

    private void onCollectionChanged(CollectionContainer.CollectionChangeEvent<Comment> event) {
        commentsDataGrid.scrollToEnd();
    }
}
