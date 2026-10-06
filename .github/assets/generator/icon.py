"""Draws the BlockGens icon as a pure vector SVG from Minecraft block textures."""
import math

from textures import texture as jar_texture


def texture(name):
    # Animated textures are vertical strips of square frames; the first frame is enough
    pixels = jar_texture('block/' + name)
    return pixels[:len(pixels[0])]


def glowing(rgba):
    r, g, b, alpha = rgba
    if r > 180 and r > b * 2:
        return min(255, round(r * 1.1)), min(255, round(g * 1.3 + 20)), b, alpha
    return rgba


def hex_color(rgba, shade=1.0):
    r, g, b, _ = rgba
    return '#%02x%02x%02x' % tuple(min(255, round(c * shade)) for c in (r, g, b))


def face(tex, origin, du, dv, shade):
    """Draws a 16x16 texture on the parallelogram origin + u * du + v * dv."""
    out = []
    n = len(tex)
    for v in range(n):
        for u in range(n):
            rgba = tex[v][u]
            if rgba[3] == 0:
                continue
            corners = [(u, v), (u + 1, v), (u + 1, v + 1), (u, v + 1)]
            points = ' '.join('%.2f,%.2f' % (origin[0] + cu * du[0] + cv * dv[0],
                                             origin[1] + cu * du[1] + cv * dv[1]) for cu, cv in corners)
            color = hex_color(rgba, shade)
            # The stroke in the same colour hides the hairline seams between neighbouring texels
            out.append('<polygon points="%s" fill="%s" stroke="%s" stroke-width="0.6"/>' % (points, color, color))
    return out


def cube(top, left, right, cx, y, texel, shades=(1.0, 0.82, 0.64)):
    """An isometric cube whose top face's back corner is at (cx, y)."""
    a, b = texel * math.cos(math.radians(30)), texel * math.sin(math.radians(30))
    back = (cx, y)
    left_corner = (cx - 16 * a, y + 16 * b)
    front = (cx, y + 32 * b)
    out = []
    out += face(top, left_corner, (a, -b), (a, b), shades[0])
    out += face(left, left_corner, (a, b), (0, texel), shades[1])
    out += face(right, front, (a, -b), (0, texel), shades[2])
    return out


def arc_arrow(cx, cy, radius, width, start, end, head):
    """A thick arc from angle start to end (degrees, clockwise from the right) with an arrow head at the end."""
    def point(angle, r):
        rad = math.radians(angle)
        return cx + r * math.cos(rad), cy + r * math.sin(rad)

    outer, inner = radius + width / 2, radius - width / 2
    sweep = 1 if end > start else 0
    large = 1 if abs(end - start) > 180 else 0
    direction = 1 if end > start else -1
    tip_angle = end + direction * math.degrees(head / radius)
    p1, p2 = point(start, outer), point(end, outer)
    p3, p4 = point(end, inner), point(start, inner)
    h1, h2, tip = point(end, outer + width * 0.55), point(end, inner - width * 0.55), point(tip_angle, radius)
    return ('M %.1f %.1f A %.1f %.1f 0 %d %d %.1f %.1f L %.1f %.1f L %.1f %.1f L %.1f %.1f L %.1f %.1f '
            'A %.1f %.1f 0 %d %d %.1f %.1f Z') % (
        *p1, outer, outer, large, sweep, *p2, *h1, *tip, *h2, *p3, inner, inner, large, 1 - sweep, *p4)


def gold_pattern(tex, size):
    cell = size / 16
    rects = []
    for v in range(16):
        for u in range(16):
            rects.append('<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s"/>' % (
                u * cell, v * cell, cell + 0.3, cell + 0.3, hex_color(tex[v][u])))
    return ('<pattern id="gold" patternUnits="userSpaceOnUse" width="%.1f" height="%.1f">%s</pattern>'
            % (size, size, ''.join(rects)))


def icon():
    size, texel = 512, 8.6
    a, b = texel * math.cos(math.radians(30)), texel * math.sin(math.radians(30))
    cube_height = 32 * b + 16 * texel
    total = cube_height + 16 * texel
    cx = 256
    top_y = (size - total) / 2 + 6

    # The lava cracks are made brighter and more yellow, so the block looks lit from the inside
    magma = [[glowing(px) for px in row] for row in texture('magma')]
    bottom = cube(magma, magma, magma, cx, top_y + 16 * texel, texel, shades=(1.08, 0.98, 0.88))
    ore = texture('diamond_ore')
    top = cube(ore, ore, ore, cx, top_y, texel)

    gold = texture('gold_block')
    centre_y = top_y + 16 * b + 8 * texel
    # Thick and close to the middle of the ore, so the cycle clearly belongs to it
    radius = 16 * a - 16
    arrows = [arc_arrow(cx, centre_y, radius, 38, 175, 310, 30),
              arc_arrow(cx, centre_y, radius, 38, -5, 130, 30)]

    magma_top = top_y + 16 * texel
    glow_y = magma_top + cube_height / 2
    outline = ' '.join('%.1f,%.1f' % p for p in (
        (cx, magma_top), (cx + 16 * a, magma_top + 16 * b), (cx + 16 * a, magma_top + 16 * b + 16 * texel),
        (cx, magma_top + cube_height), (cx - 16 * a, magma_top + 16 * b + 16 * texel),
        (cx - 16 * a, magma_top + 16 * b)))
    glow_defs = ('<radialGradient id="glow">'
                 '<stop offset="0" stop-color="#ffb000" stop-opacity="0.95"/>'
                 '<stop offset="0.45" stop-color="#ff6a00" stop-opacity="0.55"/>'
                 '<stop offset="1" stop-color="#ff3d00" stop-opacity="0"/></radialGradient>'
                 '<filter id="bloom" x="-60%" y="-60%" width="220%" height="220%">'
                 '<feGaussianBlur stdDeviation="16"/></filter>')
    # A wide soft halo, then a tight blurred copy of the block's outline right behind it
    glow_shapes = ('<ellipse cx="%d" cy="%.1f" rx="250" ry="200" fill="url(#glow)"/>'
                   '<polygon points="%s" fill="#ff8c00" filter="url(#bloom)"/>') % (cx, glow_y, outline)

    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">' % (
        size, size, size, size), '<defs>', gold_pattern(gold, 36), glow_defs, '</defs>', glow_shapes]
    arrow = '<path d="%s" fill="url(#gold)" stroke="#6b4a00" stroke-width="5" stroke-linejoin="round"/>'
    parts.append('<g>%s</g>' % ''.join(bottom))
    parts.append('<g>%s</g>' % ''.join(top))
    parts += [arrow % d for d in arrows]
    parts.append('</svg>')
    return '\n'.join(parts)
