package com.mantkowicz.light.e2e;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * A changed copy of the packed starter level (android/assets/maps/level1.tmx) for tests that need something the real
 * level doesn't have: a walls layer, mirrored tiles, another ambient light. Written to e2e/build/test-maps; it keeps
 * using the game's tiles atlas, so it can only use tiles the real level uses (take their gids with {@link #gidAt}).
 */
public final class LevelVariant {
    /** Tiled's flags in the high bits of a gid: mirrored left-right, mirrored top-bottom, mirrored on the diagonal. */
    public static final long FLIP_HORIZONTALLY = 0x80000000L, FLIP_VERTICALLY = 0x40000000L, FLIP_DIAGONALLY = 0x20000000L;

    private static final Path ROOT = Path.of(System.getProperty("repo.root", "../..")).toAbsolutePath().normalize();
    private static final Path LEVEL = ROOT.resolve("android/assets/maps/level1.tmx");
    private static final Path OUTPUT = ROOT.resolve("e2e/build/test-maps");

    private final Document document;
    private final Element map;
    private final int width;
    private final int height;

    private LevelVariant() {
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(LEVEL.toFile());
        } catch (Exception e) {
            throw new IllegalStateException("Can't read " + LEVEL, e);
        }
        map = document.getDocumentElement();
        width = Integer.parseInt(map.getAttribute("width"));
        height = Integer.parseInt(map.getAttribute("height"));
        // the copy lives elsewhere: point it back at the game's atlas
        mapProperty("atlas", OUTPUT.relativize(LEVEL.resolveSibling("tiles.atlas")).toString().replace('\\', '/'));
    }

    public static LevelVariant ofLevel1() {
        return new LevelVariant();
    }

    /**
     * Sets (or adds) a property of the map, e.g. "ambientLight".
     */
    public LevelVariant mapProperty(String name, String value) {
        Element properties = (Element) map.getElementsByTagName("properties").item(0);
        NodeList list = properties.getElementsByTagName("property");
        for (int i = 0; i < list.getLength(); i++) {
            Element property = (Element) list.item(i);
            if (property.getParentNode() == properties && name.equals(property.getAttribute("name"))) {
                property.setAttribute("value", value);
                property.removeAttribute("type");
                return this;
            }
        }
        Element property = document.createElement("property");
        property.setAttribute("name", name);
        property.setAttribute("value", value);
        properties.appendChild(property);
        return this;
    }

    /**
     * The gid (tile) a layer of the real level has on a cell, e.g. a grass tile to reuse elsewhere.
     */
    public long gidAt(String layerName, Cell cell) {
        return gids(layer(layerName))[index(cell)];
    }

    /**
     * Sets an attribute of the map element, e.g. "tilewidth".
     */
    public LevelVariant mapAttribute(String name, String value) {
        map.setAttribute(name, value);
        return this;
    }

    /**
     * Adds a tile layer on top of the others (below the objects) with the given gids on the given cells.
     */
    public LevelVariant addLayer(String name, Map<Cell, Long> tiles) {
        return addLayer(name, tiles, 1f);
    }

    /**
     * {@link #addLayer(String, Map)} with the layer's opacity (Tiled's layer property, 0 … 1).
     */
    public LevelVariant addLayer(String name, Map<Cell, Long> tiles, float opacity) {
        long[] gids = new long[width * height];
        tiles.forEach((cell, gid) -> gids[index(cell)] = gid);
        Element layer = document.createElement("layer");
        layer.setAttribute("id", String.valueOf(100 + map.getElementsByTagName("layer").getLength()));
        layer.setAttribute("name", name);
        layer.setAttribute("width", String.valueOf(width));
        layer.setAttribute("height", String.valueOf(height));
        if (opacity != 1f) {
            layer.setAttribute("opacity", String.valueOf(opacity));
        }
        Element data = document.createElement("data");
        data.setAttribute("encoding", "csv");
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < gids.length; i++) {
            csv.append(i == 0 ? "" : ",").append(gids[i]);
        }
        data.setTextContent(csv.toString());
        layer.appendChild(data);
        Node objects = map.getElementsByTagName("objectgroup").item(0);
        map.insertBefore(layer, objects);
        return this;
    }

    /**
     * Writes the variant and returns its path, for {@link GameDriver#start(String)}.
     */
    public String write(String fileName) {
        try {
            Files.createDirectories(OUTPUT);
            File file = OUTPUT.resolve(fileName).toFile();
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(file));
            return file.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalStateException("Can't write " + fileName, e);
        }
    }

    private Element layer(String name) {
        NodeList layers = map.getElementsByTagName("layer");
        for (int i = 0; i < layers.getLength(); i++) {
            Element layer = (Element) layers.item(i);
            if (name.equals(layer.getAttribute("name"))) {
                return layer;
            }
        }
        throw new IllegalArgumentException("level1 has no layer " + name);
    }

    private static long[] gids(Element layer) {
        String[] values = layer.getElementsByTagName("data").item(0).getTextContent().trim().split("\\s*,\\s*");
        long[] gids = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            gids[i] = Long.parseLong(values[i]);
        }
        return gids;
    }

    /** Tiled lists rows from the top, the game counts them from the bottom. */
    private int index(Cell cell) {
        return (height - 1 - cell.row()) * width + cell.col();
    }
}
