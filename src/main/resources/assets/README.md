# Asset Storage & Resource Management

This directory holds static media assets required by the RailFlow Desktop Application.

## Directory Structure

```
assets/
├── fonts/     # Custom modern typography (e.g. Outfit-SemiBold.ttf, Inter-Regular.ttf)
├── icons/     # Vector SVG icons (e.g. train.svg, ticket.svg, seat.svg, calendar.svg)
├── images/    # High-resolution raster images, coach schematics, promotional train banners
└── videos/    # Short looping animated clips, journey teasers, intro splash loops
```

## Asset Guidelines
1. **Icons**:
   - Prefer vector **SVG** format. Loaded seamlessly via `FlatSVGIcon`.
   - Scale automatically to any DPI screen resolution without fuzziness.
2. **Images**:
   - WebP or high-quality PNG with alpha transparency.
   - Store in `images/` and access via the classpath:
     ```java
     getClass().getResource("/assets/images/banner.png");
     ```
3. **Videos**:
   - MP4 / H.264 or sequence frame formats for short video teasers and ambient hero backgrounds.
4. **Fonts**:
   - OpenType (`.otf`) or TrueType (`.ttf`). Registered dynamically into `GraphicsEnvironment` at startup.
