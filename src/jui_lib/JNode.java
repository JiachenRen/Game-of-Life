package jui_lib;

import com.sun.istack.internal.Nullable;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

//idea, Jan 21. Chaining up the methods so they return itself, allowing syntax such as setHeight().setWidth().
//spit into JNode instances and static back_end controls.
//Don't forget to add Tables!!
//TODO: add global key event listeners.
public class JNode {

    //create getter and setters for the following. Modified Jan 26.
    public static int UNI_MENU_TEXT_SIZE = 15; //universal menu textSize TODO remove
    public static PFont UNI_FONT;
    public static String OS = System.getProperty("os.name").toLowerCase();
    static private PApplet parent;

    /*to be imported from default.txt*/
    public static boolean DISPLAY_CONTOUR;
    public static boolean CONTAINER_VISIBLE;
    public static boolean ROUNDED;
    public static boolean AUTO_TEXT_DESCENT_COMPENSATION;
    public static float CONTAINER_MARGIN_X;
    public static float CONTAINER_MARGIN_Y;
    public static float CONTAINER_SPACING;
    public static float CONTOUR_THICKNESS;
    public static float ROUNDING;
    public static float CONTEXTUAL_INIT_TEXT_PERCENTAGE;

    public static int COLOR_MODE;
    public static int BACKGROUND_COLOR;
    public static int MOUSE_PRESSED_BACKGROUND_COLOR;
    public static int MOUSE_OVER_BACKGROUND_COLOR;
    public static int CONTOUR_COLOR;
    public static int MOUSE_PRESSED_CONTOUR_COLOR;
    public static int MOUSE_OVER_CONTOUR_COLOR;
    public static int TEXT_COLOR;
    public static int MOUSE_PRESSED_TEXT_COLOR;
    public static int MOUSE_OVER_TEXT_COLOR;

    private static int recordedMousePos[];
    private static boolean mouseIsPressed;
    private static boolean initMousePosRecorded;
    private static boolean keyIsPressed;
    private static ArrayList<Displayable> displayables;

    /**
     * for now, as of May 2nd, the automatic event transferring mechanism only covers
     * mouse events, not including mouseWheel(). It does not support key events. In order
     * for JNode to work properly, it needs to be properly linked to one specific processing
     * PApplet instance's keyPressed(), keyReleased(), and mouseWheel() method with the
     * following syntax(should be located in a subclass of PApplet):
     * <p>
     * public void keyPressed(){JNode.keyPressed();}
     * public void keyReleased(){JNode.keyReleased();}
     * public void mouseWheel(){JNode.mouseWheel();}
     */
    private static boolean automaticEventTransferring = true;

    public static void init(PApplet p) {
        parent = p;
        displayables = new ArrayList<>();
        setUniFont(p.createFont(PFont.list()[1], 100));
        importStyle("default");
    }

    /**
     * loops through all JUI displayable instances, draw them where needed and
     * transfer mouse, key events wherever requested.
     */
    public static void run() {
        parent.pushStyle();
        try {
            for (int i = displayables.size() - 1; i >= 0; i--) {
                if (i > displayables.size())
                    continue; //to avoid ConcurrentModificationException TODO: does not solve the issue
                Displayable displayable = displayables.get(i);
                //parent.colorMode(displayable.colorMode); //TODO: not yet functional
                if (displayable.isVisible() && !displayable.isRelative())
                    displayable.run();
            }
            for (int i = displayables.size() - 1; i >= 0; i--) {
                Displayable displayable = displayables.get(i);
                if (displayable.getAttachedMethod() != null) {
                    displayable.getAttachedMethod().run();
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        parent.popStyle();
        if (automaticEventTransferring)
            JNode.transferInputEvents();
    }

    /**
     * TODO debug
     *
     * @since May 1st. User will not need to plug transfer the processing specific
     * mousePressed, mouseReleased, dragged and key events by hand. This is a update
     * that truly makes a difference.
     */
    private static void transferInputEvents() {
        if (parent.mousePressed) {
            if (mouseIsPressed) {
                //the mouse dragged event transferring mechanism goes here.
                if (initMousePosRecorded) {
                    if (parent.mouseX != recordedMousePos[0] || parent.mouseY != recordedMousePos[1]) {
                        JNode.mouseDragged();
                        recordedMousePos = new int[]{parent.mouseX, parent.mouseY};
                    }
                    JNode.mouseHeld();
                } else {
                    recordedMousePos = new int[]{parent.mouseX, parent.mouseY};
                    initMousePosRecorded = true;
                }
            } else {
                JNode.mousePressed();
                mouseIsPressed = true;
            }
        } else {
            initMousePosRecorded = false;
            if (mouseIsPressed) {
                JNode.mouseReleased();
                mouseIsPressed = false;
            }
        }
        //TODO key events listener

    }

    /**
     * This method would soon become one of the best features of JUI, as
     * it offers incredible customization capabilities; each individual style
     * sheet does not need to contain all the customization info; instead,
     * the user can only include the style that they are willing to change.
     * For example, a file named button_style.txt can only contain a single
     * line "mouse_pressed_background_color: 234,12,45,27", and it would be
     * considered as valid.
     *
     * @param fileName the name of your_customization_file.txt to be imported into JNode.
     * @since April 26th idea by Jiachen Ren.
     */
    public static void importStyle(String fileName) {
        String[] lines = parent.loadStrings("jui_lib/customization/" + fileName);
        System.out.println("default imported; jui_lib 2.0.1\n");
        label:
        for (String line : lines) {
            if (!line.contains(": ")) continue;
            String data = line.split(": ")[1];
            String keyWord = line.split(": ")[0];
            try {
                Field field = JNode.class.getDeclaredField(keyWord.toUpperCase());
                String fieldTypeName = field.getType().getName();
                PApplet.print(field.getName().toLowerCase() + " = ");
                switch (keyWord) {
                    case "color_mode":
                        switch (data) {
                            case "RGB":
                                field.setInt(null, PConstants.RGB);
                                break;
                            case "ARGB":
                                field.setInt(null, PConstants.ARGB);
                                break;
                            case "HSB":
                                field.setInt(null, PConstants.HSB);
                                break;
                        }
                        continue label;
                }
                switch (fieldTypeName) {
                    case "float":
                        field.setFloat(null, Float.valueOf(data));
                        PApplet.println(field.getFloat(null));
                        break;
                    case "boolean":
                        field.setBoolean(null, Boolean.valueOf(data));
                        PApplet.println(field.getBoolean(null));
                        break;
                    case "int":
                        String temp[] = data.split(",");
                        int[] rgba = new int[4];
                        for (int i = 0; i < rgba.length; i++)
                            rgba[i] = Integer.valueOf(temp[i]);
                        int color = parent.color(rgba[0], rgba[1], rgba[2], rgba[3]);
                        field.setInt(null, color);
                        PApplet.println(color);
                        break;
                    case "String":
                        PApplet.println(field.getName());
                        break;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                /*learned "|" April 26th. Wow.*/
                e.printStackTrace();
            }
        }
    }

    public static void add(Displayable displayable) {
        //parent.noLoop();
        //if (displayables.contains(displayable)) return;
        displayables.add(displayable);
        //parent.loop();
    }


    public static void addAll(ArrayList<Displayable> displayables) {
        for (Displayable displayable : displayables) {
            add(displayable);
        }
    }

    public static void addAll(Displayable... displayables) {
        for (Displayable displayable : displayables) {
            add(displayable); //unlimited number of parameters! Learned Jan 28th.
        }
    }

    public static void remove(String id) {
        ArrayList<? extends Displayable> selected = JNode.get(id);
        for (int i = selected.size() - 1; i >= 0; i--) {
            Displayable reference = selected.get(i);
            remove(reference);
        }
    }

    /**
     * @param obj the displayable object to be removed.
     */
    public static void remove(Displayable obj) {
        //removing from displayables ArrayList, which contains reference to all objects
        parent.noLoop();
        /*this is here to prevent ConcurrentModificationException when another thread tries to
        remove a displayable object while the main loop of processing is iterating through it*/
        /*modified March 8th*/
        for (int i = displayables.size() - 1; i >= 0; i--)
            if (displayables.get(i) == obj) displayables.remove(i);

        //removing from containers, call should be passed to every single sub-containers to remove all objects.
        for (int i = displayables.size() - 1; i >= 0 && i < displayables.size(); i--) {
            Displayable displayable = displayables.get(i);
            if (displayable instanceof Container) {
                ((Container) displayable).remove(obj);
            }
        }

        parent.loop();
    }

    //faster version of remove()
    public static void ripOff(Displayable displayable) {
        displayables.remove(displayable);
    }

    public static ArrayList<? extends Displayable> get(String id) {
        ArrayList<Displayable> selected = new ArrayList<>();
        for (Displayable displayable : displayables)
            if (Objects.equals(displayable.getId(), id))
                selected.add(displayable);
        return selected;
    }

    //keyboard/mouse input action receivers. Bug fixed Jan 28th 11:26PM.
    public static void mousePressed() {
        for (int i = displayables.size() - 1; i >= 0; i--) {
            Displayable displayable = displayables.get(i);
            if (!(displayable instanceof MenuDropdown))
                if (!displayable.isVisible()) continue;
            displayable.mousePressed();
            if (i >= displayables.size()) break;
        }
    }

    public static void mouseReleased() {
        for (int i = displayables.size() - 1; i >= 0; i--) {
            Displayable displayable = displayables.get(i);
            if (!(displayable instanceof MenuDropdown))
                if (!displayable.isVisible()) continue;
            displayable.mouseReleased();
            if (i >= displayables.size()) break; //fixed!!! June 9th, breakthrough!
        }
    }

    public static void mouseDragged() {
        for (int i = displayables.size() - 1; i >= 0; i--) {
            Displayable displayable = displayables.get(i);
            if (!(displayable instanceof MenuDropdown))
                if (!displayable.isVisible()) continue;
            displayable.mouseDragged();
            if (i >= displayables.size()) break;
        }
    }

    public static void mouseHeld() {
        for (int i = displayables.size() - 1; i >= 0; i--) {
            Displayable displayable = displayables.get(i);
            if (!(displayable instanceof MenuDropdown))
                if (!displayable.isVisible()) continue;
            displayable.mouseHeld();
            if (i >= displayables.size()) break;
        }
    }

    public static void mouseWheel() {
        for (Displayable displayable : displayables) {
            if (!(displayable instanceof MenuDropdown))
                if (!displayable.isVisible()) continue;
            displayable.mouseWheel();
        }
    }

    public static void keyPressed() {
        for (int i = displayables.size() - 1; i >= 0; i--) {
            Displayable displayable = displayables.get(i);
            if (!(displayable instanceof MenuDropdown))
                if (!displayable.isVisible()) continue;
            if (displayable.getClass().getInterfaces().length != 0) {
                if (displayable instanceof KeyControl) {
                    KeyControl c = (KeyControl) displayable;
                    c.keyPressed();
                    if (i >= displayables.size()) break;
                }
            }
        }
    }

    public static void keyReleased() {
        for (int i = displayables.size() - 1; i >= 0; i--) {
            Displayable displayable = displayables.get(i);
            if (!(displayable instanceof MenuDropdown))
                if (!displayable.isVisible()) continue;
            if (displayable.getClass().getInterfaces().length != 0) {
                if (displayable instanceof KeyControl) {
                    KeyControl c = (KeyControl) displayable;
                    c.keyReleased();
                    if (i >= displayables.size()) break;
                }
            }
        }
    }

    public static ArrayList<Displayable> getById(String id) {
        ArrayList<Displayable> selected = new ArrayList<>();
        for (Displayable displayable : displayables)
            if (displayable.getId().equals(id))
                selected.add(displayable);
        return selected;
    }

    public static ArrayList<Displayable> getDisplayables() {
        return displayables;
    }

    public static void setUniFont(PFont textFont) {
        UNI_FONT = textFont;
    }

    public static void setUniMenuTextSize(int textSize) {
        UNI_MENU_TEXT_SIZE = textSize;
    }

    public static PApplet getParent() {
        return parent;
    }

    /*built in tools for the JUI library*/
    public static int[] getRgb(int rgb) {
        return new int[]{(int) parent.red(rgb), (int) parent.green(rgb), (int) parent.blue(rgb)};
    }

    public static int resetAlpha(int rgb, float alpha) {
        int[] temp = getRgb(rgb);
        return parent.color(temp[0], temp[1], temp[2], alpha);
    }

    /*seed oscillation*/
    public static float oscSeed() {
        return (float) Math.random() * 2.0f - 1.0f;
    }

    /**
     * TODO
     *
     * @param keyword
     * @return
     */
    public static ArrayList<Displayable> search(String keyword) {
        ArrayList<Displayable> searchResult = new ArrayList<>();
        for (Displayable displayable : displayables) {
            if (displayable.getId().contains(keyword))
                searchResult.add(displayable);
        }
        return searchResult;
    }

    /**
     * reference: https://processing.org/discourse/beta/num_1202486379.html
     * Draw a dashed line with given set of dashes and gap lengths.
     * x0 starting x-coordinate of line.
     * y0 starting y-coordinate of line.
     * x1 ending x-coordinate of line.
     * y1 ending y-coordinate of line.
     * spacing array giving lengths of dashes and gaps in pixels;
     * an array with values {5, 3, 9, 4} will draw a line with a
     * 5-pixel dash, 3-pixel gap, 9-pixel dash, and 4-pixel gap.
     * if the array has an odd number of entries, the values are
     * recycled, so an array of {5, 3, 2} will draw a line with a
     * 5-pixel dash, 3-pixel gap, 2-pixel dash, 5-pixel gap,
     * 3-pixel dash, and 2-pixel gap, then repeat.
     */
    public static void dashLine(float x0, float y0, float x1, float y1, float[] spacing) {
        float distance = PApplet.dist(x0, y0, x1, y1);
        float[] xSpacing = new float[spacing.length];
        float[] ySpacing = new float[spacing.length];
        float drawn = 0.0f;  // amount of distance drawn

        if (distance > 0) {
            int i;
            boolean drawLine = true; // alternate between dashes and gaps

            for (i = 0; i < spacing.length; i++) {
                xSpacing[i] = PApplet.lerp(0, (x1 - x0), spacing[i] / distance);
                ySpacing[i] = PApplet.lerp(0, (y1 - y0), spacing[i] / distance);
            }

            i = 0;
            while (drawn + PApplet.mag(xSpacing[i], ySpacing[i]) < distance) {
                if (drawLine) {
                    getParent().line(x0, y0, x0 + xSpacing[i], y0 + ySpacing[i]);
                }
                x0 += xSpacing[i];
                y0 += ySpacing[i];

                /* Add distance "drawn" by this line or gap */
                drawn = drawn + PApplet.mag(xSpacing[i], ySpacing[i]);
                i = (i + 1) % spacing.length;  // cycle through array
                drawLine = !drawLine;  // switch between dash and gap
            }
        }
    }

    public static void setAutomaticEventTransferring(boolean temp) {
        JNode.automaticEventTransferring = temp;
    }

    /**
     * calculates the difference between 2 colors according to human perception.
     * #reference: https://stackoverflow.com/questions/2103368/color-logic-algorithm
     *
     * @param color1 the first color
     * @param color2 the second color
     * @return the difference between two colors
     */
    public static float colorDistance(float color1, float color2) {
        int redMean = (int) ((getParent().red((int) color1) + getParent().red((int) color2)) / 2);
        int r = (int) (getParent().red((int) color1) - getParent().red((int) color2));
        int g = (int) (getParent().green((int) color1) - getParent().green((int) color2));
        int b = (int) (getParent().blue((int) color1) - getParent().blue((int) color2));
        return PApplet.sqrt((((512 + redMean) * r * r) >> 8) + 4 * g * g + (((767 - redMean) * b * b) >> 8));
    }
}