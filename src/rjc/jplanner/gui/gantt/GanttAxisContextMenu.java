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

package rjc.jplanner.gui.gantt;

import java.util.function.BiConsumer;

import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/*********************** Menu for changing and adding Gantt timeline axes ************************/
/*************************************************************************************************/

class GanttAxisContextMenu extends ContextMenu
{
  private static final DateTime               SAMPLE       = DateTime.now();                                    // common preset preview
  private static final GanttAxisLabelFormat[] WEEK_FORMATS = { GanttAxisLabelFormat.DAY_NUMBER,
      GanttAxisLabelFormat.DAY_WITH_MONTH, GanttAxisLabelFormat.WEEK_NUMBER, GanttAxisLabelFormat.WEEK_PREFIXED,
      GanttAxisLabelFormat.WEEK_WITH_YEAR };                                                                    // date labels precede week numbers

  /**************************************** constructor ******************************************/
  GanttAxisContextMenu( Gantt gantt, GanttAxis axis )
  {
    // add menu items for each of the axis label formats
    BiConsumer<GanttAxisInterval, GanttAxisLabelFormat> change = ( interval, format ) -> gantt.setAxisSettings( axis,
        interval, format );
    var intervals = intervalMenu( "Interval", axis, change );
    var formats = formatMenu( "Format", axis.getInterval(), axis, change );
    boolean canAdd = gantt.getAxisCount() < Gantt.MAX_AXES;
    var above = canAdd
        ? intervalMenu( "Add axis above", null, ( interval, format ) -> gantt.addAxis( axis, true, interval, format ) )
        : new Menu( "Add axis above" );
    var below = canAdd
        ? intervalMenu( "Add axis below", null, ( interval, format ) -> gantt.addAxis( axis, false, interval, format ) )
        : new Menu( "Add axis below" );
    above.setDisable( !canAdd );
    below.setDisable( !canAdd );

    var remove = new MenuItem( "Remove axis" );
    remove.setDisable( gantt.getAxisCount() <= 1 );
    remove.setOnAction( event -> gantt.removeAxis( axis ) );
    getItems().addAll( intervals, formats, new SeparatorMenuItem(), above, below, remove );
  }

  /**************************************** intervalMenu *****************************************/
  private Menu intervalMenu( String text, GanttAxis selected,
      BiConsumer<GanttAxisInterval, GanttAxisLabelFormat> action )
  {
    // create menu for each of the axis intervals
    var menu = new Menu( text );
    var times = new Menu( "Time intervals" );
    for ( var interval : GanttAxisInterval.values() )
    {
      MenuItem item;
      if ( selected == null )
        item = formatMenu( interval.getText(), interval, null, action );
      else
      {
        var check = new CheckMenuItem( interval.getText() );
        check.setSelected( selected.getInterval() == interval );
        check.setOnAction( event ->
        {
          // preserve a suitable format, otherwise change both values to the interval's default
          var format = selected.getLabelFormat();
          action.accept( interval, format.isRelevantTo( interval ) ? format : interval.getDefaultFormat() );
        } );
        item = check;
      }
      ( interval.isTimeInterval() ? times : menu ).getItems().add( item );
    }
    menu.getItems().add( times );
    return menu;
  }

  /***************************************** formatMenu ******************************************/
  private Menu formatMenu( String text, GanttAxisInterval interval, GanttAxis selected,
      BiConsumer<GanttAxisInterval, GanttAxisLabelFormat> action )
  {
    // create menu for each of the axis label formats
    var menu = new Menu( text );
    var presets = interval == GanttAxisInterval.WEEK ? WEEK_FORMATS : GanttAxisLabelFormat.values();
    for ( var preset : presets )
    {
      if ( !preset.isRelevantTo( interval ) )
        continue;
      String label = preset.getText() + " (" + preset.format( SAMPLE ) + ")";
      MenuItem item;
      if ( selected == null )
        item = new MenuItem( label );
      else
      {
        var check = new CheckMenuItem( label );
        check.setSelected( selected.getLabelFormat() == preset );
        item = check;
      }
      item.setOnAction( event -> action.accept( interval, preset ) );
      menu.getItems().add( item );
    }
    return menu;
  }
}
