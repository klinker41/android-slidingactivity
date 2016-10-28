/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.sliding.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

public class SampleActivity extends AppCompatActivity {

    public static final String ARG_USE_EXPANSION = "arg_use_expansion";
    public static final String ARG_EXPANSION_LEFT_OFFSET = "arg_left_offset";
    public static final String ARG_EXPANSION_TOP_OFFSET = "arg_top_offset";
    public static final String ARG_EXPANSION_VIEW_WIDTH = "arg_view_width";
    public static final String ARG_EXPANSION_VIEW_HEIGHT = "arg_view_height";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        findViewById(R.id.show_normal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNormalExample();
            }
        });

        findViewById(R.id.show_fullscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullscreenExample();
            }
        });

        findViewById(R.id.show_menu_item).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenuItemExample();
            }
        });

        findViewById(R.id.show_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageExample();
            }
        });

        findViewById(R.id.show_dark).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDarkExample();
            }
        });

        findViewById(R.id.show_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFabExample();
            }
        });

        findViewById(R.id.show_no_header).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoHeaderExample();
            }
        });

        findViewById(R.id.show_custom_header).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomHeaderExample();
            }
        });

        findViewById(R.id.show_talon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTalonExample();
            }
        });
    }

    private void showNormalExample() {
        startActivity(addExpansionArgs(new Intent(this, NormalActivity.class)));
    }

    private void showFullscreenExample() {
        startActivity(addExpansionArgs(new Intent(this, FullscreenActivity.class)));
    }

    private void showMenuItemExample() {
        startActivity(addExpansionArgs(new Intent(this, MenuItemActivity.class)));
    }

    private void showImageExample() {
        startActivity(addExpansionArgs(new Intent(this, ImageActivity.class)));
    }

    private void showDarkExample() {
        startActivity(addExpansionArgs(new Intent(this, DarkActivity.class)));
    }

    private void showFabExample() {
        startActivity(addExpansionArgs(new Intent(this, FabActivity.class)));
    }

    private void showNoHeaderExample() {
        startActivity(addExpansionArgs(new Intent(this, NoHeaderActivity.class)));
    }

    private void showCustomHeaderExample() {
        startActivity(addExpansionArgs(new Intent(this, CustomHeaderActivity.class)));
    }

    private void showTalonExample() {
        startActivity(addExpansionArgs(new Intent(this, TalonActivity.class)));
    }

    public Intent addExpansionArgs(Intent intent) {
        if (((CheckBox) findViewById(R.id.use_expansion_check)).isChecked()) {
            intent.putExtra(ARG_USE_EXPANSION, true);

            View expansionView = findViewById(R.id.expansion_view);

            int location[] = new int[2];
            expansionView.getLocationInWindow(location);

            intent.putExtra(ARG_EXPANSION_LEFT_OFFSET, location[0]);
            intent.putExtra(ARG_EXPANSION_TOP_OFFSET, location[1]);
            intent.putExtra(ARG_EXPANSION_VIEW_WIDTH, expansionView.getWidth());
            intent.putExtra(ARG_EXPANSION_VIEW_HEIGHT, expansionView.getHeight());
        }

        return intent;
    }

}
