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

package rjc.jplanner.plan;

/*************************************************************************************************/
/********************************** Quantity of time with units **********************************/
/*************************************************************************************************/

public class TimeSpan
{
  private double m_num;
  private char   m_units;

  /**************************************** constructor ******************************************/
  public TimeSpan()
  {
    // construct default time-span
    m_num = 0.0;
    m_units = 'd';
  }

  /**************************************** constructor ******************************************/
  public TimeSpan( String str )
  {
    // construct time-span from string, start with default
    this();
  }

  /**************************************** constructor ******************************************/
  public TimeSpan( double num, char units )
  {
    // construct time-span from parameters, rounding number based on units

  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return time-span as string
    return m_num + " " + m_units;
  }

  /****************************************** getUnits *******************************************/
  public char getUnits()
  {
    // return time-span units
    return m_units;
  }

  /****************************************** getNumber ******************************************/
  public double getNumber()
  {
    // return time-span number
    return m_num;
  }

  /******************************************** minus ********************************************/
  public TimeSpan minus()
  {
    // return a new time-span with minus number
    return new TimeSpan( -m_num, m_units );
  }

}
