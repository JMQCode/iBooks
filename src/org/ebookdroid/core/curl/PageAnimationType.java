package org.ebookdroid.core.curl;

import org.ebookdroid.EBookDroidApp;
import com.greenlemonmobile.app.ebook.R;
import org.ebookdroid.core.SinglePageController;

import org.emdev.utils.enums.ResourceConstant;

public enum PageAnimationType implements ResourceConstant {

    NONE(R.string.pref_animation_type_none, true),

    CURLER(R.string.pref_animation_type_curler_simple, false),

    CURLER_DYNAMIC(R.string.pref_animation_type_curler_dynamic, false),

    CURLER_NATURAL(R.string.pref_animation_type_curler_natural, false),

    SLIDER(R.string.pref_animation_type_slider, true),

    FADER(R.string.pref_animation_type_fader, true),

    SQUEEZER(R.string.pref_animation_type_squeezer, true);

    /** The resource value. */
    private final String resValue;

    private final boolean hardwareAccelSupported;

    /**
     * Instantiates a new page animation type.
     *
     * @param resValue
     *            the res value
     */
    private PageAnimationType(final int resId, final boolean hardwareAccelSupported) {
        this.resValue = EBookDroidApp.context.getString(resId);
        this.hardwareAccelSupported = hardwareAccelSupported;
    }

    /**
     * Gets the resource value.
     *
     * @return the resource value
     */
    public String getResValue() {
        return resValue;
    }

    public boolean isHardwareAccelSupported() {
        return hardwareAccelSupported;
    }

    public static PageAnimator create(final PageAnimationType type, final SinglePageController singlePageDocumentView) {
        if (type != null) {
            switch (type) {
                case CURLER:
                    return new SinglePageSimpleCurler(singlePageDocumentView);
                case CURLER_DYNAMIC:
                  return new SinglePageDynamicCurler(singlePageDocumentView);
                case CURLER_NATURAL:
                    return new SinglePageNaturalCurler(singlePageDocumentView);
                case SLIDER:
                    return new SinglePageSlider(singlePageDocumentView);
                case FADER:
                    return new SinglePageFader(singlePageDocumentView);
                case SQUEEZER:
                    return new SinglePageSqueezer(singlePageDocumentView);
                default:
                    break;
            }
        }
        return new SinglePageDefaultSlider(singlePageDocumentView);
    }
}
