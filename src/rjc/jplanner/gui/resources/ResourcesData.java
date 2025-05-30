/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.jplanner.gui.resources;

import javafx.geometry.Pos;
import rjc.jplanner.plan.Resource.FIELD;
import rjc.jplanner.plan.Resources;
import rjc.table.data.TableData;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/**************************** Table data source for showing resources ****************************/
/*************************************************************************************************/

public class ResourcesData extends TableData
{
  private Resources  m_resources;
  private CellVisual m_disabledVisual;

  /**************************************** constructor ******************************************/
  public ResourcesData( Resources resources )
  {
    // construct data model
    m_resources = resources;
    setRowCount( m_resources.size() );
    setColumnCount( FIELD.MAX.ordinal() );

    // visual for disabled cells
    m_disabledVisual = new CellVisual();
    m_disabledVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
    m_disabledVisual.textPaint = null;
  }

  /****************************************** getValue *******************************************/
  @Override
  public Object getValue( int dataColumn, int dataRow )
  {
    // return blank for corner cell
    if ( dataRow == HEADER && dataColumn == HEADER )
      return null;

    // return column header
    if ( dataRow == HEADER )
      return FIELD.values()[dataColumn];

    // return row header
    if ( dataColumn == HEADER )
      return dataRow;

    // otherwise return value from resources array
    return m_resources.get( dataRow ).getValue( dataColumn );
  }

  /***************************************** getVisual *******************************************/
  @Override
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // return disabled-cells-visual for non-initials field if initials is blank
    if ( dataRow > HEADER && dataColumn > FIELD.Initials.ordinal() )
      if ( m_resources.get( dataRow ).isBlank() )
        return m_disabledVisual;

    // otherwise return default cell visuals
    CellVisual visual = super.getVisual( dataColumn, dataRow );
    if ( dataRow > HEADER && dataColumn == FIELD.Comment.ordinal() )
      visual.textAlignment = Pos.CENTER_LEFT;
    else
      visual.textAlignment = Pos.CENTER;

    return visual;
  }

  /****************************************** setValue *******************************************/
  @Override
  protected String setValue( int dataColumn, int dataRow, Object newValue, Boolean commit )
  {
    // test if value can/could be set
    return m_resources.get( dataRow ).setValue( dataColumn, newValue, commit );
  }

}
