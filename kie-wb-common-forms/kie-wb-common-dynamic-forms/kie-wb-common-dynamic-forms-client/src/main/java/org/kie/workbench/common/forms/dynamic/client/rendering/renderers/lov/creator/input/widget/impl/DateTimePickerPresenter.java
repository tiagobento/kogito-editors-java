/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.forms.dynamic.client.rendering.renderers.lov.creator.input.widget.impl;

import java.util.Date;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.common.client.api.IsElement;
import org.jboss.errai.common.client.dom.HTMLElement;
import org.uberfire.mvp.Command;

@Dependent
public class DateTimePickerPresenter implements DateTimePickerPresenterView.Presenter,
                                                IsElement {

    private Date date;

    private DateTimePickerPresenterView view;

    private Command valueChangeCommand;
    private Command hideCommand;

    @Inject
    public DateTimePickerPresenter(DateTimePickerPresenterView view) {
        this.view = view;
        view.init(this);
    }

    public void init(Command valueChangeCommand, Command hideCommand) {
        this.valueChangeCommand = valueChangeCommand;
        this.hideCommand = hideCommand;
    }

    @Override
    public HTMLElement getElement() {
        return view.getElement();
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public Date getDate() {
        return date;
    }

    public void show() {
        view.show();
    }

    @Override
    public void notifyDateChange(Date date) {

        this.setDate(date);

        if(valueChangeCommand != null) {
            valueChangeCommand.execute();
        }
    }

    @Override
    public void notifyHide() {

        if(hideCommand != null) {
            hideCommand.execute();
        }
    }
}
