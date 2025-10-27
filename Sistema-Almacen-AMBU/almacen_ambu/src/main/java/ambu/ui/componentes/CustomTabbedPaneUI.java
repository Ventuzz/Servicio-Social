package ambu.ui.componentes;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.plaf.basic.BasicTabbedPaneUI;

/*----------------------------------
    Campo para personalizar pestañas
 -----------------------------------*/
public class CustomTabbedPaneUI extends BasicTabbedPaneUI {

    private final Color tabBackgroundColor = new Color(25, 25, 25);
    private final Color selectedTabColor = new Color(35, 35, 35);
    private final Color neonIndicatorColor = new Color(20, 255, 120);

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabAreaInsets.left = 0;
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(0,0,0,0);
    }
    
    @Override
    protected LayoutManager createLayoutManager() {
        return new CustomTabLayout();
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(isSelected ? selectedTabColor : tabBackgroundColor);
        g2.fillRect(x, y, w, h);

        if (isSelected) {
            g2.setColor(neonIndicatorColor);
            g2.fillRect(x, y, 5, h); 
        }
        
        g2.dispose();
    }
    
    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                             int tabIndex, String title, Rectangle textRect, boolean isSelected) {

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rightPadding = 15;

        int newX = textRect.x + textRect.width - metrics.stringWidth(title) - rightPadding;
        int newY = textRect.y + (textRect.height - metrics.getHeight()) / 2 + metrics.getAscent();

        if (isSelected) {
            g2.setColor(Color.WHITE);
        } else {
            g2.setColor(Color.LIGHT_GRAY);
        }

        g2.drawString(title, newX, newY);
        
        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {

    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {

    }
    
    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return new Insets(15, 20, 15, 20); // Aumentamos el padding para más espacio vertical
    }

    public class CustomTabLayout extends TabbedPaneLayout {

    @Override
    public void calculateTabRects(int tabPlacement, int tabCount) {

        super.calculateTabRects(tabPlacement, tabCount);

        if (tabPlacement == LEFT) {
            int tabPaneWidth = tabPane.getWidth();
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
            int availableWidth = tabPaneWidth - tabAreaInsets.left - tabAreaInsets.right;

            for (int i = 0; i < tabCount; i++) {
                rects[i].width = availableWidth;
                rects[i].x = tabAreaInsets.left;
            }
        }
    }
}
}