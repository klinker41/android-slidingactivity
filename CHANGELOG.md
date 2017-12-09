## Changelog

### Version 1.5.2:
- Remove `final` modifier, per request

### Version 1.5.1:
- Update dependencies and build tools for SDK 26

### Version 1.5.0:
- Add ability to put a transparent overlay over the header image
- Add method to get the FAB instance
- Add ability to change the text color of the header text

### Version 1.4.4:
- Update dependencies and build tools for SDK 25

### Version 1.4.3:
- Update to `PeekView` 1.2.2 to improve blurring support and remove `RenderScript` dependency in the example app

### Version 1.4.2:
- We need the libraries min SDK to be 15 since that is what the blur on `PeekView` uses.

### Version 1.4.1:
- Update to `PeekView` 1.2.0 for background blur support.

### Version 1.4.0:
- Allow `SlidingActivity` to work with `PeekView` (https://github.com/klinker24/Android-3DTouch-PeekView)

### Version 1.3.2:
- Fix a crash on sw600dp tablets

### Version 1.3.1:
- Make sure 720dp tablets have the new layout too

### Version 1.3.0:
- Tablets have had a cool non-full-screen layout in portrait mode. Apply that layout to tablets landscape mode, as well as phone's landscape mode, if space allows.

### Version 1.2.3:
- Don't allow text to overlap FAB when title is long
- Add Day/Night theme

### Version 1.2.2:
- Improve logic behind header color tint

### Version 1.2.1:
- Fixed custom headers wrapping content instead of matching parent

### Version 1.2.0:
- Added the ability to set custom header views

### Version 1.1.1:
- Fade in the header text after it is set
- Remove the padding from the content view because it caused a weird coloring effect

### Version 1.1.0:
- Add an 'Inbox' style expansion animation to the window

### Version 1.0.1:
- Fix issue with the expansion when there is no header
- Allow for keyboard showing on `SlidingActivity`

### Version 1.0.0:
- Initial release and feature set
