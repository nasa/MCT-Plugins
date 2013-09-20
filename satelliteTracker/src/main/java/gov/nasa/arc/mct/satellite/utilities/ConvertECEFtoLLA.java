/*******************************************************************************
 * Mission Control Technologies, Copyright (c) 2009-2012, United States Government
 * as represented by the Administrator of the National Aeronautics and Space 
 * Administration. All rights reserved.
 *
 * The MCT platform is licensed under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 *
 * MCT includes source code licensed under additional open source licenses. See 
 * the MCT Open Source Licenses file included with this distribution or the About 
 * MCT Licenses dialog available at runtime from the MCT Help menu for additional 
 * information. 
 *******************************************************************************/
package gov.nasa.arc.mct.satellite.utilities;

/*
 * Converting from ECEF to LLA is a complicated enough process, I made it its own
 * class (and a public one, rather than an inner class, since I'll need this stuff
 * later for my own purposes)
 * 
 * TODO:
 * Currently, SatTrak has the functionality to convert from LLA to ECEF (this was
 * desined so, as at the time I thought I had no need to convert from ECEF to LLA);
 * perhaps I should move all of that code here, and then change the class name to
 * ConvertECEFandLLA.
 * 
 * Note: this class is used wrt the MercatorProjection view, as the incoming feeds
 * are in ECEF, so the MercatorPanel converts these values to lat and lon
 */
public class ConvertECEFtoLLA {

	@SuppressWarnings("unused")
	private double EARTH_A, EARTH_B, EARTH_F, EARTH_Ecc, EARTH_Esq;
	
	
	public ConvertECEFtoLLA() {
		setWGS84Values();
	}
	
	private void setWGS84Values() {
		double  wgs84a, wgs84b, wgs84f;

          wgs84a         =  6378.137;
          wgs84f         =  1.0/298.257223563;
          wgs84b         =  wgs84a * ( 1.0 - wgs84f );

          setEarthConstants(wgs84a, wgs84b);
	}
	
	/*	Sets Earth Constants as globals */
	private void setEarthConstants(double ai, double bi) {
		double  f,ecc, eccsq, a,b;

           a        =  ai;
           b        =  bi;

           f        =  1-b/a;
           eccsq    =  1 - b*b/(a*a);
           ecc      =  Math.sqrt(eccsq);

           EARTH_A   =  a;
           EARTH_B   =  b;
           EARTH_F   =  f;
           EARTH_Ecc =  ecc;
           EARTH_Esq =  eccsq;
	}
	
	/* compute the radii at the geodetic latitude lat (in degrees)
	 * 		input:  lat       geodetic latitude in degrees
 	 *		output: an array 3 long where  r,  rn,  rm are in  in km
	 */
	private double[] radCur( double lati) {
		
		double[] rrnrm = new double[3];
		double a, b, asq, bsq, eccsq, ecc,
		       lat, clat, slat, dsq, d, rn, rm, rho,
		       z, rsq, r, dtr;
		
		 dtr = Math.PI/180.0;
		
		 a     = EARTH_A;
	     b     = EARTH_B;

	     asq   = a*a;
	     bsq   = b*b;
	     eccsq  =  1 - bsq/asq;
	     ecc = Math.sqrt(eccsq);

	     lat   =  lati;

	     clat  =  Math.cos(dtr*lat);
	     slat  =  Math.sin(dtr*lat);

	     dsq   =  1.0 - eccsq * slat * slat;
	     d     =  Math.sqrt(dsq);

	     rn    =  a/d;
	     rm    =  rn * (1.0 - eccsq ) / dsq;

	     rho   =  rn * clat;
	     z     =  (1.0 - eccsq ) * rn * slat;
	     rsq   =  rho*rho + z*z;
	     r     =  Math.sqrt( rsq );

	     rrnrm[0]  =  r;
	     rrnrm[1]  =  rn;
	     rrnrm[2]  =  rm;

	     return  rrnrm ;
		
	}
	
	
	

	
	/*        geocentric latitude to geodetic latitude

    	Input:
              	flatgc    geocentric latitude deg.
              	altkm     altitide in km
    	ouput:
              	flatgd    geodetic latitude in deg

	 */
	private double gc2gd(double flatgci, double altkmi) {
		double dtr   = Math.PI/180.0;
	     double rtd   = 1/dtr;

	     double  flatgd,flatgc,altkm;
	     double[]  rrnrm = new double[3];
	     double  rn,ecc, esq;
	     double  tlat;
	     double  altnow,ratio;

	     //geodGBL();

	     flatgc=  flatgci;
	     altkm =  altkmi;
	     
	     ecc   =  EARTH_Ecc;
	     esq   =  ecc*ecc;

//	             approximation by stages
//	             1st use gc-lat as if is gd, then correct alt dependence

	     altnow  =  altkm;

	     rrnrm   =  radCur (flatgc);
	     rn      =  rrnrm[1];
	     
	     ratio   = 1 - esq*rn/(rn+altnow);

	     tlat    = Math.tan(dtr*flatgc) / ratio;
	     flatgd  = rtd * Math.atan(tlat);

//	        now use this approximation for gd-lat to get rn etc.

	     rrnrm   =  radCur ( flatgd );
	     rn      =  rrnrm[1];

	     ratio   =  1  - esq*rn/(rn+altnow);
	     tlat    =  Math.tan(dtr*flatgc)/ratio;
	     flatgd  =  rtd * Math.atan(tlat);

	     return  flatgd;

     }

	//  physical radius of earth from geodetic latitude
	private double rearth(double lati) {
		double   lat;
        lat   =  lati;
        return  radCur(lat)[0]; //we are returning 'r' from the rrnrm vector
	}

	
	
	/**
	 * 
	 * @param xvec xyz ECEF locations in km
	 * @return a double array (of size three)
	 *     <UL>
	 *        <LI> [0] geodetic latitude in deg
	 *        <LI> [1] longitude in deg in the range of [-180,180]
	 *        <LI> [2] altitude in km
	 *     </UL>
	 */
	public double[] ecefToLLA( double[] xvec ) {
		
		double  dtr =  Math.PI/180.0;
		double  flatgc,flatn,dlat;
		double  rnow,rp;
		double  x,y,z,p;
		double tangd;
		double  testval;

		double  rn,esq;
		double  clat,slat;
		double[] rrnrm = new double[3];

		double flat,flon,altkm;
		double[] llhvec = new double[3];

	     esq    =  EARTH_Esq;

	     x      = xvec[0];
	     y      = xvec[1];
	     z      = xvec[2];

	     rp     = Math.sqrt ( x*x + y*y + z*z );

	     flatgc = Math.asin ( z / rp )/dtr;

	     testval= Math.abs(x) + Math.abs(y);
	     if ( testval < 1.0e-10)
	         {flon = 0.0; }
	     else
	         {flon = Math.atan2 ( y,x )/dtr; } 
	     if (flon < 0.0 )  { flon = flon + 360.0; }

	     p      =  Math.sqrt( x*x + y*y );

	     //on pole special case
	     if ( p < 1.0e-10 )
	       {  
	          flat = 90.0;
	          if ( z < 0.0 ) { flat = -90.0; }

	          altkm = rp - rearth(flat);
	          llhvec[0]  = flat;
	          llhvec[1]  = flon;
	          llhvec[2]  = altkm;

	          return  llhvec;
	        }

//	        first iteration, use flatgc to get altitude 
//	        and alt needed to convert gc to gd lat.

	     rnow  =  rearth(flatgc);
	     altkm =  rp - rnow;
	     flat  =  gc2gd (flatgc,altkm);
	          
	     rrnrm =  radCur(flat);
	     rn    =  rrnrm[1];

	     for(int kount=0; kount< 5 ; kount++ )
	       {
	           slat  =  Math.sin(dtr*flat);
	           tangd =  ( z + rn*esq*slat ) / p;
	           flatn =  Math.atan(tangd)/dtr;

	           dlat  =  flatn - flat;
	           flat  =  flatn;
	           clat  =  Math.cos( dtr*flat );

	           rrnrm =  radCur(flat);
	           rn    =  rrnrm[1];

	           altkm =  (p/clat) - rn;

	           if ( Math.abs(dlat) < 1.0e-12 ) { break; }

	       }
	     
	          llhvec[0]  = flat;
	          llhvec[1]  =((flon+180)%360) - 180;	//convert longitude from [0,360] range, to [-180,180] range.
	          llhvec[2]  = altkm;

	          return  llhvec ;

	     }
	
	
	
	/*Here is some correct test-data
		Name: ISS (ZARYA)             
		Latitude[Deg]: -37.3187132539739
		Longitude[Deg]: 140.32208346479482
		Altitude [m]: 427277.4083292503
		ECEF x[km]:-3913.8231010403706
		ECEF y[km]:3246.771107703494
		ECEF z[km]:-3850.581024475044
	 */	
	public static void main(String[] args) {
		ConvertECEFtoLLA convert = new ConvertECEFtoLLA();
		
		//ecefVec must be in units of (km)
		double[] ecefVec = { -3913.8231010403706, 3246.771107703494, -3850.581024475044 };
		double[] lla = convert.ecefToLLA(ecefVec);
		
		System.out.println("ECEF: \nx=" + ecefVec[0] + "\ny=" + ecefVec[1] + "\nz=" + ecefVec[2] +"\n");
		System.out.println("Lat: "+ lla[0] + "\nLon: "+lla[1] +"\nHeight(km): " + lla[2]);

	}

}
