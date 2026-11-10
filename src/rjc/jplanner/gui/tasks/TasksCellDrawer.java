/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.gui.tasks;

import javafx.geometry.Rectangle2D;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/************************* Draws task titles with summary collapse boxes **************************/
/*************************************************************************************************/

final class TasksCellDrawer extends CellDrawer
{
  private static final double COLLAPSE_BOX_SIZE = 11.0; // logical pixels across the summary box

  /*************************************** getCollapseBox ****************************************/
  Rectangle2D getCollapseBox()
  {
    // only draw a collapse box for summary tasks in the title column
    if ( dataColumn != FIELD.Title.ordinal() || dataRow <= 0 || !( (TasksData) view.getData() ).isSummary( dataRow ) )
      return null;

    double zoom = view.getZoom().get();
    double size = Math.floor( Math.min( COLLAPSE_BOX_SIZE * zoom, h - 4.0 * zoom ) );
    if ( size < 3.0 )
      return null;
    double left = x + getZoomTextInsets().getLeft() - TasksData.TITLE_CONTROL_WIDTH * zoom;
    return new Rectangle2D( Math.floor( left ), Math.floor( y + ( h - size ) / 2.0 ), size, size );
  }

  /**************************************** drawContent ******************************************/
  @Override
  protected void drawContent()
  {
    // the library's multiline text layout requires positive space after title padding
    boolean hasTextSpace = true;
    if ( dataColumn == FIELD.Title.ordinal() )
    {
      var insets = getZoomTextInsets();
      hasTextSpace = w - insets.getLeft() - insets.getRight() >= 1.0;
    }
    if ( hasTextSpace )
      super.drawContent();
    var box = getCollapseBox();
    if ( box == null )
      return;

    // the base drawer clips both the title and its box to the visible part of the cell
    gc.setStroke( Colours.CELL_BORDER.darker() );
    gc.setLineWidth( 1.0 );
    gc.strokeRect( box.getMinX() + 0.5, box.getMinY() + 0.5, box.getWidth() - 1.0, box.getHeight() - 1.0 );

    double middleX = Math.floor( box.getMinX() + box.getWidth() / 2.0 ) + 0.5;
    double middleY = Math.floor( box.getMinY() + box.getHeight() / 2.0 ) + 0.5;
    double inset = Math.max( 2.0, Math.floor( box.getWidth() / 4.0 ) );
    gc.strokeLine( box.getMinX() + inset + 0.5, middleY, box.getMaxX() - inset - 0.5, middleY );

    var visibility = ( (TasksView) view ).getRowVisibility();
    if ( visibility.isCollapsed( ( (TasksData) view.getData() ).getTasks().get( dataRow ) ) )
      gc.strokeLine( middleX, box.getMinY() + inset + 0.5, middleX, box.getMaxY() - inset - 0.5 );
  }
}
