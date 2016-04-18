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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.klinker.android.sliding.SlidingActivity;

/**
 * Activity demonstrating showing menu items on the sliding content, this is achieved in the same
 * manor as you would handle it for any type of activity.
 */
public class MenuItemActivity extends SlidingActivity {

    /**
     * Initialize our values, this is overridden instead of onCreate as it should be in all
     * sliding activities.
     * @param savedInstanceState the saved state.
     */
    @Override
    public void init(Bundle savedInstanceState) {
        setTitle(R.string.submit_feedback);
        setPrimaryColors(
                getResources().getColor(R.color.menu_item_activity_primary),
                getResources().getColor(R.color.menu_item_activity_primary_dark)
        );

        setContent(R.layout.activity_feedback);

        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Intent intent = getIntent();
        if (intent.getBooleanExtra(SampleActivity.ARG_USE_EXPANSION, false)) {
            expandFromPoints(
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_LEFT_OFFSET, 0),
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_TOP_OFFSET, 0),
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_VIEW_WIDTH, 0),
                    intent.getIntExtra(SampleActivity.ARG_EXPANSION_VIEW_HEIGHT, 0)
            );
        }
    }

    /**
     * Creates the options menu.
     * @param menu the menu.
     * @return true.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_menu_item, menu);
        return true;
    }

    /**
     * Handles the options item selected event.
     * @param item the item selected.
     * @return true.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_bug:
                Toast.makeText(this, R.string.bug, Toast.LENGTH_SHORT).show();
                break;
        }

        return true;
    }

}
