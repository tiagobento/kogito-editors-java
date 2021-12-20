/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.forms.common.rendering.client.widgets.integerBox;

import com.google.gwt.user.client.ui.IsWidget;
import org.kie.workbench.common.forms.common.rendering.client.widgets.FormWidget;

public interface IntegerBoxView extends IsWidget,
                                        FormWidget<Long> {

    void setPresenter(IntegerBox presenter);

    void setValue(String value);

    void setEnabled(boolean enabled);

    String getTextValue();

    void setId(String id);

    void setPlaceholder(String placeholder);

    void setMaxLength(int maxLength);
}