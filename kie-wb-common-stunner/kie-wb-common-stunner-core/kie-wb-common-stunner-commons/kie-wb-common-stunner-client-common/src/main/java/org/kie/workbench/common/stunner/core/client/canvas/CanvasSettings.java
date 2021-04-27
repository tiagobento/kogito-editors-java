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

package org.kie.workbench.common.stunner.core.client.canvas;

import java.util.Optional;

public class CanvasSettings {

    private final Optional<CanvasSize> canvasSize;
    private final boolean isHiDPIEnabled;

    public CanvasSettings(final boolean isHiDPIEnabled) {
        this.canvasSize = Optional.empty();
        this.isHiDPIEnabled = isHiDPIEnabled;
    }

    public CanvasSettings(final int width,
                          final int height,
                          final boolean isHiDPIEnabled) {
        this.canvasSize = Optional.of(new CanvasSize(width, height));
        this.isHiDPIEnabled = isHiDPIEnabled;
    }

    public Optional<CanvasSize> getCanvasSize() {
        return canvasSize;
    }

    public boolean isHiDPIEnabled() {
        return isHiDPIEnabled;
    }

    public static class CanvasSize {

        private final int width;
        private final int height;

        public CanvasSize(final int width,
                          final int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}