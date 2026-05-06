package com.studio.statusvault.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.studio.statusvault.R;
import com.studio.statusvault.utils.WalkthroughHelper;

public class WalkthroughActivity extends BaseActivity {

    private final int[] walkthroughImages = {
            R.drawable.w_1,
            R.drawable.w_2,
            R.drawable.w_3
    };

    private final int[] walkthroughStepTitleRes = {
            R.string.walkthrough_step_1_title,
            R.string.walkthrough_step_2_title,
            R.string.walkthrough_step_3_title
    };

    private final int[] walkthroughStepBodyRes = {
            R.string.walkthrough_step_1_body,
            R.string.walkthrough_step_2_body,
            R.string.walkthrough_step_3_body
    };

    private int currentPage = 0;
    private ImageView imageWalkthrough;
    private TextView textStepNumber;
    private TextView textStepTitle;
    private TextView textStepBody;
    private ImageView indicatorOne;
    private ImageView indicatorTwo;
    private ImageView indicatorThree;
    private MaterialButton buttonPrevious;
    private MaterialButton buttonNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkthrough);

        imageWalkthrough = findViewById(R.id.imageWalkthrough);
        textStepNumber = findViewById(R.id.textWalkthroughStepNumber);
        textStepTitle = findViewById(R.id.textWalkthroughStepTitle);
        textStepBody = findViewById(R.id.textWalkthroughStepBody);
        indicatorOne = findViewById(R.id.indicatorOne);
        indicatorTwo = findViewById(R.id.indicatorTwo);
        indicatorThree = findViewById(R.id.indicatorThree);
        buttonPrevious = findViewById(R.id.buttonWalkthroughPrevious);
        buttonNext = findViewById(R.id.buttonWalkthroughNext);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentPage > 0) {
                    currentPage--;
                    renderCurrentPage();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("page", 0);
        }

        buttonPrevious.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                renderCurrentPage();
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (currentPage < walkthroughImages.length - 1) {
                currentPage++;
                renderCurrentPage();
                return;
            }
            WalkthroughHelper.markShown(this);
            finish();
        });

        renderCurrentPage();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("page", currentPage);
    }

    private void renderCurrentPage() {
        imageWalkthrough.setImageResource(walkthroughImages[currentPage]);
        textStepNumber.setText(getString(R.string.walkthrough_step_number, currentPage + 1));
        textStepTitle.setText(walkthroughStepTitleRes[currentPage]);
        textStepBody.setText(walkthroughStepBodyRes[currentPage]);
        if (currentPage == 0) {
            buttonPrevious.setVisibility(View.GONE);
        } else {
            buttonPrevious.setVisibility(View.VISIBLE);
        }
        buttonNext.setText(currentPage == walkthroughImages.length - 1
                ? R.string.walkthrough_done
                : R.string.walkthrough_next);

        indicatorOne.setImageResource(currentPage == 0
                ? R.drawable.bg_walkthrough_dot_active
                : R.drawable.bg_walkthrough_dot_inactive);
        indicatorTwo.setImageResource(currentPage == 1
                ? R.drawable.bg_walkthrough_dot_active
                : R.drawable.bg_walkthrough_dot_inactive);
        indicatorThree.setImageResource(currentPage == 2
                ? R.drawable.bg_walkthrough_dot_active
                : R.drawable.bg_walkthrough_dot_inactive);
    }
}
