package games.RubiksCube;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Hashtable;

import games.BoardVector;

/**
 * Class CubeState represents a certain cube state. It comes in four different types (member {@code type}):
 * <ul>
 * <li> <b>COLOR_P</b>: color representation of a 2x2x2 pocket cube
 * <li> <b>COLOR_R</b>: color representation of a 3x3x3 Rubik's cube
 * <li> <b>TRAFO_P</b>: transformation representation for pocket cube
 * <li> <b>TRAFO_R</b>: transformation representation for Rubik's cube
 * </ul>
 * Its member {@link #fcol} has 24 or 48 elements for pocket and Rubik's cube, resp. 
 * It stores in the case of a <b>color representation</b> type in {@code fcol[i]} the face color of 
 * cubie face i. The color is one out of {0,1,2,3,4,5} for colors {w,b,o,y,g,r} = 
 * {white,blue,orange,yellow,green,red}. For transformation representation type TRAFO_* see below.
 * <p>
 * Its member {@link #sloc} has 24 or 48 elements for pocket and Rubik's cube, resp. 
 * It stores the sticker location: {@code sloc[i]} holds the location of the sticker which is at 
 * face {@code i} for the solved cube.
 * <p>
 * Its member {@code lastTwist} stores the last twist action (U,L,F) performed on this cube
 * (ID if none or not known).
 * <p>
 * Its member {@code twistSequence} stores the twist sequence needed for generating this 
 * state from the default cube {@code def}, e.g. "L2U1" for {@code def.LTw(2).UTw(1)}. <br>
 * ({@code twistSequence=""} if the twist sequence is not known, e.g. because CubeState was 
 * generated by color symmetry transformation) 
 * <p>
 * CubeState has member functions {@link #uTr(int)}, {@link #lTr(int)}, {@link #fTr(int)} for whole-cube 
 * rotations and {@link #UTw(int)}, {@link #LTw(int)}, {@link #FTw(int)} for cube twists. 
 * <p>
 * More details:
 * <p>
 * Two CubeState objects are <b>equal</b>, if their {@code fcol[]} arrays have the same content. 
 * To get the corresponding sameness-behaviour when using {@link HashSet#add(Object)} 
 * and {@link Hashtable#get(Object)}, the functions {@link #equals(Object)} 
 * <b>AND</b> {@link #hashCode()} from class Object have to be 
 * overridden here.
 * <p>
 * If CubeState {@code trafo} is of <b>transformation representation</b> type <b>TRAFO_*</b>, then  
 * {@code trafo.fcol[i]} stores the location (number) of the parent cubie face.
 * That is, if {@code trafo}  is applied to another color-type CubeState cS, the color of cS.fcol[i] 
 * will become {@code cS.fcol[trafo.fcol[i]]}. See {@link #apply(CubeState)}.
 * <p> 
 * The numbering in {@link #fcol} and {@link #sloc} runs around the cube faces in the following order: U, L, F, D, R, B. 
 * Within the first three faces, we start at the wbo-cubie; within the last three faces we start
 * at the ygr-cubie. Within each face we march around in counter-clockwise
 * orientation. This gives for the pocket cube the following numbering:
 * <pre>
 *       3  2				
 *       0  1				
 * 5  4  8 11 18 17 23 22
 * 6  7  9 10 19 16 20 21
 *      14 13				
 *      15 12				
 * </pre>
 * 
 * @author Wolfgang Konen, TH Koeln, 2018-2020
 */
public class CubeState implements Serializable {
	
	public static enum Type {COLOR_P,COLOR_R,TRAFO_P,TRAFO_R}
	public static enum Twist {ID,U,L,F}
	private static enum Cor {a,b,c,d,e,f,g,h}
	
	/**
	 * {@code fcol} is the face color array with 24 (<b>COLOR_P</b>) or 48 (<b>COLOR_R</b>) elements. <br> 
	 * {@code fcol[i]} holds the face color for cubie face (sticker) with number {@code i} (<b>COLOR_*</b>).<br>
	 * {@code fcol[i]} holds the parent location for cubie face (sticker) with number {@code i} (<b>TRAFO_*</b>).
	 */
	public int[] fcol; 
	
	/**
	 * {@code sloc} is the sticker location, an array with 24 (<b>COLOR_P</b>) or 48 (<b>COLOR_R</b>) elements. <br>
	 * {@code sloc[i]} holds the location of the sticker which is at face {@code i} for the solved cube.  
	 */
	public int[] sloc;

	Type type = Type.COLOR_P;
	Twist lastTwist = Twist.ID;
	int lastTimes = 0;
	String twistSeq = "";   // e.g. "L2U1" means that 
							//		(CubeState.makeCubeState()).LTw(2).UTw(1) 
							// produces this. ("": not known).
	int minTwists = -1;		// minimum number of twists needed to solve this state (-1: not known)
	
	/**
	 * change the version ID for serialization only if a newer version is no longer 
	 * compatible with an older one (older .agt.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long  serialVersionUID = 12L;

	/**
	 * {@code invF, invL, invU} are the <b>inverse</b> transformations for {@link #FTw()}, {@link #LTw()}, {@link #UTw()}.<br>
	 * That is, {@code fcol[invF[i]]} is the color which cubie face {@code i} gets after {@link #FTw()} transformation.
	 */
	private static int[] invF = null,
			 			 invL = null,
			 			 invU = null;
	//                                		0          4         8            12           16           20
	private static final int[] 	invF_2x2 = {18,19,2,3,  1, 5,6,0, 11, 8, 9,10, 12, 7, 4,15, 16,17,13,14, 20,21,22,23},
						 		invL_2x2 = { 9, 1,2,8,  7, 4,5,6, 14,15,10,11, 12,13,21,22, 16,17,18,19, 20, 3, 0,23},
						 		invU_2x2 = { 3, 0,1,2, 22,23,6,7,  5, 9,10, 4, 12,13,14,15, 16,11, 8,19, 20,21,17,18};
	//
	// use the following line once on a default TRAFO_P CubeState t_cs to generate int[] invL above:
	//			return t_cs.uTr().FTw().uTr().uTr().uTr();   	// L(x) = u^3(F(u(x))) 
	// use the following line once on a default TRAFO_P CubeState t_cs to generate int[] invU above:
	//			return t_cs.lTr().lTr().lTr().FTw().lTr();   	// U(x) = l(F(l^3(x))) 
	
	/**
	 * {@code tforF, tforL, tforU} are the <b>forward</b> transformations needed in {@link #FTw()}, {@link #LTw()}, {@link #UTw()}.<br>
	 * That is, {@code tforF[i]} is the new location for a sticker which was in location {@code i} prior to a {@link #FTw()} transformation.<br>
	 * {@code tforF, tforL, tforU} are filledvia method {@link #generateForwardTs()}, 
	 * which is called once at startup time from the constructor {@link ArenaTrainCube#ArenaTrainCube(String, boolean)}.
	 */
	private static int[] tforF = null,
						 tforL = null,
						 tforU = null;
	
	/**
	 * generate the <b>inverse</b> transformations {@link #invF}, {@link #invL} and {@link #invU}. 
	 */
	public static void generateInverseTs() {
		switch (CubeConfig.cubeType) {
		case POCKET: 
			invF = invF_2x2;
			invL = invL_2x2;
			invU = invU_2x2;
			break;
		case RUBIKS:
			throw new RuntimeException("Not yet done!");
		}
		
	}
	
	/**
	 * generate the <b>forward</b> transformations {@link #tforF} (and similar) from {@link #invF} (and similar). 
	 */
	public static void generateForwardTs() {
		tforF = new int[invF.length];
		tforL = new int[invF.length];
		tforU = new int[invF.length];
		// Since 3x applying F is F^3 = F^{-1}, it also holds that 3x applying F^{-1} gives F^9 = F.
		// So we can calculate tforF by 3x appling invF: 
		for (int i=0; i<invF.length; i++) {
			tforF[i] = invF[invF[invF[i]]];
			tforL[i] = invL[invL[invL[i]]];
			tforU[i] = invU[invU[invU[i]]];
		}
	}
	
	/**
	 * Construct a new cube of Type {@code type} in default (solved) state
	 * @param type	the cube type
	 */
	public CubeState(Type type) {
		this.type = type;
		switch(type) {
		case COLOR_P: 
			this.fcol = new int[] {0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5};
			break;
		case COLOR_R:
			this.fcol = new int[] {0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,
								   3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5};
			break;
		case TRAFO_P:
			// fcol[i] holds the parent of location i under the trafo represented by this CubeState object.
			// Initially this trafo is the id transformation for the pocket cube.
			this.fcol = new int[24];
			for (int i=0; i<fcol.length; i++) this.fcol[i] = i;
			break;
		case TRAFO_R:
			// fcol[i] holds the parent of location i under the trafo represented by this CubeState object.
			// Initially this trafo is the id transformation for the Rubik's cube.
			this.fcol = new int[48];
			for (int i=0; i<fcol.length; i++) this.fcol[i] = i;
			break;
		}
		// sloc[i], the location of each face i, is in any case the default sequence 0,...,n where n=fcol.length
		this.sloc = new int[this.fcol.length];
		for (int i=0; i<sloc.length; i++) this.sloc[i] = i;
	}
	
	/**
	 * Construct a new cube of <b>color representation</b> type from a board vector {@code bvec}
	 * @param boardVector	the board vector, see {@link #getBoardVector()} for the different types
	 */
	public CubeState(BoardVector boardVector) {
		CubeStateFactory csFactory = new CubeStateFactory();
		int[] bvec = boardVector.bvec;
		switch(bvec.length) {
		case 24: 	// boardvecType == CUBESTATE, 2x2x2
			this.type = Type.COLOR_P;
			this.fcol = bvec.clone();	
			this.sloc = boardVector.aux.clone();
			break;
		case 26: 	// boardvecType == CUBEPLUSACTION, 2x2x2
			this.type = Type.COLOR_P;
			this.fcol = new int[24];
			System.arraycopy(bvec, 0, this.fcol, 0, 24);
			this.sloc = boardVector.aux.clone();
			break;
		case 49: 	// boardvecType == STICKER, 2x2x2
			this.type = Type.COLOR_P;
			this.sloc = slocFromSTICKER(bvec);	
			CubeState def = csFactory.makeCubeState(Type.COLOR_P);
			this.fcol = new int[def.fcol.length];
			for (int i=0; i<24; i++) this.fcol[sloc[i]] = def.fcol[i];		
			break;
		case 48:	// boardvecType == CUBESTATE, 3x3x3
			this.type = Type.COLOR_R;
			this.fcol = bvec.clone();
			break;
		case 50: 	// boardvecType == CUBEPLUSACTION, 3x3x3
			this.type = Type.COLOR_R;
			this.fcol = new int[48];
			System.arraycopy(bvec, 0, this.fcol, 0, 48);
			break;
		default:
			throw new RuntimeException("Case bvec.length = "+bvec.length+" not yet implemented.");
		}
	}
	
	/**
	 * Copy constructor
	 */
	public CubeState(CubeState cs) {
		this.type = cs.type;
		this.lastTwist = cs.lastTwist;
		this.lastTimes = cs.lastTimes;
		this.twistSeq = cs.twistSeq;
		this.minTwists = cs.minTwists;
		this.fcol = cs.fcol.clone();
		this.sloc = cs.sloc.clone();
	}

	//
	// factory methods are now in CubeStateFactory
	//
//	abstract public CubeState makeCubeState();
//
//	abstract public CubeState makeCubeState(Type type);
//
//	@Deprecated
//	abstract public CubeState makeCubeState(BoardVector boardVector);
//
//	abstract public CubeState makeCubeState(CubeState other);
	
	/**
	 * Helper for CubeState(BoardVector): 
	 * Given a board vector in STICKER representation, reconstruct member {@code sloc}.
	 * @param bvec	the board vector
	 * @return <b>int[] sloc</b>. sloc[i] holds the new location of sticker i which is at location i in the default cube.
	 */
	private int[] slocFromSTICKER(int[] bvec) {
		int[] sloc = new int[24];
		final 							// this array is found with the help of Table 3 in notes-WK-RubiksCube.docx:
		int[][] C = {{ 0,  4,  8},		// 1st row: the locations for a1,a2,a3
					 { 1, 11, 18},		// 2nd row: the locations for b1,b2,b3
					 { 2, 17, 23},		// and so on ...
					 { 3, 22,  5},
					 {12, 16, 20},
					 {13, 19, 10},
					 {14,  9,  7},
					 {15,  6, 21}};
		int cor,fac;
		for (int z=0; z<8; z++) {
			int[] corfac = getCornerAndFace(z,bvec);
			cor=corfac[0];
			fac=corfac[1];
			for (int i=0; i<3; i++) {
				sloc[ C[z][i] ] = C[cor][(fac+i)%3];
			}
		}
		return sloc;
	}
	
	/**
	 * Helper for slocFromSTICKERS:
	 * Given the index z of a tracker sticker, return from {@code bvec} (the board vector in the STICKER representation) 
	 * the corner and face where this sticker z is found.
	 * 
	 * @param z index from {0,...,7} of a tracked sticker
	 * @param bvec the board vector
	 * @return <b>int[2]</b> with the first element being the corner index {0,...,7} of the corner a,...,h and  the
	 * 		second element being the face index {0,1,2} for the face values {1,2,3} found in bvec.
	 * <p>
	 * Details: We need a little index arithmetic to account for the fact that {@code bvec} represents a 7x7 array, but 
	 * the index z is for all 8 stickers (including the ever-constant 4th sticker of the ygr-cubie (corner e)).
	 */
	private int[] getCornerAndFace(int z, int[] bvec) {
		int[] corfac = {4,0};	// the values for sticker z=4 (ygr-cubie, which stays always in place)
		int column=0;
		
		// index arithmetic, part one
		if (z<4) column=z;
		else if (z==4) return corfac;	// the ygr-cubie case
		else column=z-1;				// cases z=5,6,7 address column 4,5,6 of the STICKER board
		
		// find (row number, value) of the only non-zero element in 'column': 
		int nonzero = 0;
		int rv=0;
		for (int r=0; r<7; r++) {
			rv = bvec[r*7+column];
			if (rv!=0) {
				nonzero++;
				corfac[0] = (r<4) ? r : (r+1);		// index arithmetic, part two
				corfac[1] = rv - 1;
			}
		}
		assert (nonzero==1) : "Oops, there are "+nonzero+" elements non-zero in column "+z+", but there should be exactly 1!";
		
		return corfac;
	}
	
	// 
	// The following methods are currently only valid for 2x2x2 Pocket Cube.
	// (They need to be generalized later to the 3x3x3 case.)
	//
	
	// TODO: The whole cube rotations need later to be extended to transform also this.sloc. But for the moment 
	// this is not needed, because whole-cube rotations come only into play if we use color symmetries.
	
	/**
	 * Whole-cube rotation counter-clockwise around the u-face
	 */
	private CubeState uTr() {
		int i;
		// fcol(invT[i]) is the color which cubie face i gets after transformation:
		int[] invT = {3,0,1,2,22,23,20,21,5,6,7,4,13,14,15,12,10,11,8,9,19,16,17,18};
		int[] tmp = this.fcol.clone();
		//for (i=0; i<invT.length; i++) tmp[i] = this.fcol[invT[i]];
		for (i=0; i<invT.length; i++) this.fcol[i] = tmp[invT[i]]; 
		return this;
	}

	/**
	 * Whole-cube rotation counter-clockwise around the f-face
	 */
	private CubeState fTr() {
		int i;
		// fcol(invT[i]) is the color which cubie face i gets after transformation:
		int[] invT = {18,19,16,17,1,2,3,0,11,8,9,10,6,7,4,5,15,12,13,14,21,22,23,20};
		int[] tmp = this.fcol.clone();
		//for (i=0; i<invT.length; i++) tmp[i] = this.fcol[invT[i]];
		for (i=0; i<invT.length; i++) this.fcol[i] = tmp[invT[i]]; 
		return this;
	}

	/**	
	 * Whole-cube rotation counter-clockwise around the l-face
	 */
	private CubeState lTr() {
		return this.fTr().uTr(3).fTr(3);
	}
	
	/**
	 * Whole-cube rotation, {@code times} * 90� counter-clockwise around the u-face
	 */
	public CubeState uTr(int times) {
		for (int i=0; i<times; i++) this.uTr();
		return this;
	}
	
	/**
	 * Whole-cube rotation, {@code times} * 90� counter-clockwise around the l-face
	 */
	public CubeState lTr(int times) {
		for (int i=0; i<times; i++) this.lTr();
		return this;
	}
	
	/**
	 * Whole-cube rotation, {@code times} * 90� counter-clockwise around the f-face
	 */
	public CubeState fTr(int times) {
		for (int i=0; i<times; i++) this.fTr();
		return this;
	}
	
	
	
	/**
	 * 	 Counter-clockwise twist of the U-face
	 */
	private CubeState UTw() {
		int i;
		int[] tmp = this.fcol.clone();
		//for (i=0; i<invU.length; i++) tmp[i] = this.fcol[invU[i]];
		for (i=0; i<invU.length; i++) this.fcol[i] = tmp[invU[i]]; 
		tmp = this.sloc.clone();
		for (i=0; i<tforU.length; i++) this.sloc[i] = tforU[tmp[i]]; 
		return this;
	}
	
	/**
	 * 	 Counter-clockwise twist of the L-face
	 */
	private CubeState LTw() {
		int i;
		int[] tmp = this.fcol.clone();
		//for (i=0; i<invL.length; i++) tmp[i] = this.fcol[invL[i]];
		for (i=0; i<invL.length; i++) this.fcol[i] = tmp[invL[i]]; 
		tmp = this.sloc.clone();
		for (i=0; i<tforL.length; i++) this.sloc[i] = tforL[tmp[i]]; 
		return this;
	}

	/**
	 * 	 Counter-clockwise twist of the F-face
	 */
	private CubeState FTw() {
		int i;
		int[] tmp = this.fcol.clone();
		//for (i=0; i<invF.length; i++) tmp[i] = this.fcol[invF[i]];
		for (i=0; i<invF.length; i++) this.fcol[i] = tmp[invF[i]]; 
		tmp = this.sloc.clone();
		for (i=0; i<tforF.length; i++) this.sloc[i] = tforF[tmp[i]]; 
		return this;
	}
	
	/**
	 * U-face twist, counter-clockwise with {@code times} * 90 degrees
	 */
	public CubeState UTw(int times) {
		for (int i=0; i<times; i++) this.UTw();
		this.twistSeq = this.twistSeq + "U"+times;
		this.lastTwist = Twist.U;
		this.lastTimes = times;
		return this;
	}
	
	/**
	 * L-face twist, counter-clockwise with {@code times} * 90 degrees
	 */
	public CubeState LTw(int times) {
		for (int i=0; i<times; i++) this.LTw();
		this.twistSeq = this.twistSeq + "L"+times;
		this.lastTwist = Twist.L;
		this.lastTimes = times;
		return this;
	}
	
	/**
	 * F-face twist, counter-clockwise with {@code times} * 90 degrees 
	 */
	public CubeState FTw(int times) {
		for (int i=0; i<times; i++) this.FTw();
		this.twistSeq = this.twistSeq + "F"+times;
		this.lastTwist = Twist.F;
		this.lastTimes = times;
		return this;
	}
	
	// CAUTION: This method is not yet extended for member sloc. But apply(trafo) is only needed in case 
	// of color symmetries.
	/**
	 * Apply transformation {@code trafo} to this
	 * @param trafo a {@link CubeState} object of type TRAFO_P or TRAFO_R
	 * @return the transformed 'this'
	 */
	public CubeState apply(CubeState trafo) {
		assert(trafo.type==Type.TRAFO_P || this.type==Type.TRAFO_R) : "Wrong type in apply(trafo) !";
		int i;
		int[] tmp = this.fcol.clone();
		//for (i=0; i<fcol.length; i++) tmp[i] = this.fcol[trafo.fcol[i]];
		for (i=0; i<fcol.length; i++) this.fcol[i] = tmp[trafo.fcol[i]]; 
		return this;		
	}
	
	/**
	 * Apply color transformation cT to {@code this}. {@code this} has to be of 
	 * type COLOR_P or COLOR_R. 
	 * @param cT color transformation
	 * @return the transformed 'this'
	 */
	public CubeState applyCT(ColorTrafo cT) {
		assert(this.type==Type.COLOR_P || this.type==Type.COLOR_R) : "Wrong type in apply(cT) !";
		int[] tmp = this.fcol.clone();
		for (int i=0; i<fcol.length; i++) this.fcol[i] = cT.fcol[tmp[i]];
		return this;		
	}
	
	/**
	 * Locate the cubie with the colors of {@link CubieTriple} {@code tri} in {@code this}. 
	 * {@code this} has to be of type COLOR_P or COLOR_R.
	 * @param tri
	 * @return a {@link CubieTriple} whose member {@code loc} carries the location of the cubie with 
	 * 		   the colors of {@code tri}.
	 */
	public CubieTriple locate(CubieTriple tri) {
		CubieTriple where = new CubieTriple(tri);
		assert(this.type==Type.COLOR_P || this.type==Type.COLOR_R) : "Wrong type in apply() !";
		//            0           4          8          12         16          20 
		int[] left = {4,11,17,22, 8,3,21,14, 0,7,13,18, 20,19,9,6, 12,23,1,10, 16,15,5,2};
		int[] right= {8,18,23,5, 0,22,15,9, 4,14,19,1, 16,10,7,21, 20,2,11,13, 12,6,3,17};
		int rig;
		switch(tri.ori) {
		case CLOCK: 
			for (int i=0; i<fcol.length; i++) {
				if (fcol[i]==tri.col[0]) {
					where.loc[0]=i;
					rig = right[i];
					if (fcol[rig]==tri.col[1]) {
						where.loc[1]=rig;
						rig = right[rig];
						if (fcol[rig]==tri.col[2]) {
							where.loc[2]=rig;
							return where; 
						}
					}
				}
			}
			break;
		case COUNTER:
			throw new RuntimeException("Case COUNTER not yet implemented");
		}
		throw new RuntimeException("Invalid cube, we should not arrive here!");
	}

	/**
	 * There are three possible board vector types, depending on {@link CubeConfig#boardVecType} 
	 * <ul> 
	 * <li> <b>CUBESTATE</b>: the face color array of the cube, i.e. member {@link #fcol}
	 * <li> <b>CUBEPLUSACTION</b>: like CUBESTATE, but with two more int values added: the ordinal of the last twist and 
	 * 		the number of quarter turns in this twist
	 * <li> <b>STICKER</b>: similar to the coding suggested by [McAleer2018], we track the location of eight stickers 
	 * 		(one face of each cubie) when the cube is twisted away from its original position
	 * </ul>
	 * Detail STICKER: The coding (cell numbering) of the 7x7 stickers field: 
	 * <pre>
	 *           0  1  2  3  4  5  6
	 *       a  00 01 02 03 04 05 06
	 *       b  07 08 09 10 11 12 13
	 *       c  14 15 16 17 18 19 20
	 *       d  21 22 23 24 25 26 27
	 *       e  28 29 30 31 32 33 34
	 *       f  35 36 37 38 39 40 41
	 *       g  42 43 44 45 46 47 48    
	 * </pre>
	 * 
	 * @return an int[] vector representing the 'board' state (= cube state)
	 *
	 * NOTE: Currently, the implementation is only valid for 2x2x2 cube
	 */
	public BoardVector getBoardVector() {
		int[] bvec;
		switch (CubeConfig.boardVecType) {
		case CUBESTATE: 
			bvec = fcol.clone();
			break;
		case CUBEPLUSACTION:
			bvec = new int[fcol.length+2];
			System.arraycopy(this.fcol, 0, bvec, 0, fcol.length);
			bvec[fcol.length] = this.lastTwist.ordinal();
			bvec[fcol.length+1] = this.lastTimes;
			break;
		case STICKER:
			final int[] orig = {0,1,2,3,13,14,15}; 	// the original locations of the tracked stickers
			final Cor cor[] = {Cor.a,Cor.b,Cor.c,Cor.d,Cor.a,Cor.d,Cor.h,Cor.g,Cor.a,Cor.g,Cor.f,Cor.b,Cor.e,Cor.f,Cor.g,Cor.h,Cor.e,Cor.c,Cor.b,Cor.f,Cor.e,Cor.h,Cor.d,Cor.c};
			final int[] face = {1,1,1,1,2,3,2,3,3,2,3,2,1,1,1,1,2,2,3,2,3,3,2,3};
			int[][] board = new int[7][7];
			int column;
			for (int i=0; i<7; i++) {		// set in every column i (sticker) the row cell specified by 'cor' 
											// to the appropriate face value:
				column = cor[sloc[orig[i]]].ordinal();
				//assert column!=4;	// should not be the ygr-cubie
				if (column>4) column = column-1;
				//assert column<7;
				board[column][i] = face[sloc[orig[i]]]; 
			}
			
			// copy to linear bvec according to STICKER coding specified above
			bvec = new int[7*7];
			for (int j=0, k=0; j<7; j++)
				for (int i=0; i<7; i++,k++)
					bvec[k] = board[j][i];
			break;
		default: 
			throw new RuntimeException("Unallowed value in switch boardVecType");
		}
		return new BoardVector(bvec,sloc);   // return a BoardVector with aux = sloc (needed to reconstruct CubeState from BoardVector)
	}
	
	public CubeState clearLast() {
		this.lastTwist = Twist.ID;
		this.lastTimes = 0;
		return this;
	}
	
	public CubeState print() {
		System.out.println(this.toString());
		return this;
	}
	
	/** 
	 * Return a one-line string representing this object: Print a 
	 * 
	 */
	public String toString() {
		DecimalFormat form = new DecimalFormat("00");
		String s = "";
		switch(this.type) {
		case TRAFO_P: 
    		for (int i=0; i<fcol.length; i++) {
    			if (i%4==0) s = s + "|";
    			s = s + form.format(fcol[i]);
    		}
    		break;
		default:
    		for (int i=0; i<fcol.length; i++) {
    			if (i%4==0) s = s + "|";
    			s = s + fcol[i];
    		}
    		break;
		}
		s = s + "|";  
		return s;	
	}
	
	public String getTwistSeq() {
		return this.twistSeq;
	}
	
	/**
	 * Check that {@code this.twistSeq} matches with {@code this.fcol}.
	 * 
	 * @return true, if applying {@code this.twistSeq} to the solved cube yields the  
	 * same cube state as stored in {@code this.fcol}.<br>
	 * If {@code this.twistSeq=""} (not known), then return always true. 
	 */
	public boolean assertTwistSequence() {
		CubeStateFactory csFactory = new CubeStateFactory();
		CubeState tst = csFactory.makeCubeState();
		Twist T=Twist.ID;
		int times;
		String tw = this.twistSeq;
		if (tw.equals("")) // 'this' is from color transformation, we cannot perform check:  
			return true;
//		if (twistSeq.equals("L2U1")) {
//			int dummy=1;
//		}
		while(tw.length()>0) {
			if (tw.startsWith("U")) T=Twist.U;
			if (tw.startsWith("L")) T=Twist.L;
			if (tw.startsWith("F")) T=Twist.F;
			assert (tw.length()>1);
			times = (int)(tw.charAt(1)-48);
			assert (1<=times && times<=3);
			switch(T) {
			case U: tst.UTw(times); break;
			case L: tst.LTw(times); break;
			case F: tst.FTw(times); break;
			}
			tw =  (tw.length()>2) ? tw.substring(2,tw.length()) : "";				
		}
		return this.equals(tst);
	}
	
	/**
	 * Checks whether elements of members fcol, sloc and type are the same in {@code this} and {@code other}.
	 * (This differs from {@link Object#equals(Object)}, since the latter tests, whether 
	 * the objects are the same, not their content.)
	 */
	public boolean isEqual(CubeState other) {
		if (this.type!=other.type) return false;
		for (int i=0; i<fcol.length; i++) {
			if (this.fcol[i]!=other.fcol[i]) 
				return false;
		}     		
		for (int i=0; i<sloc.length; i++) {
			if (this.sloc[i]!=other.sloc[i]) 
				return false;
		}     		
		return true;
	}
	
	/**
	 * It is important that {@link Object#equals(Object)} is overwritten here, so that objects
	 * of class CubeState which have the same elements in fcol[] are considered as
	 * equal. The operation equals is the one that HashSet::add() relies on
	 * 
	 * @see #hashCode()
	 * @see CubeStateMap#countDifferentStates()
	 */
	@Override
	public boolean equals(Object other) {
		assert (other instanceof CubeState) : "Object other is not of class CubeState";
		return isEqual((CubeState) other);
	}
	
	/**
	 * Like with {@link CubeState#equals(Object)}, it is equally important that {@link Object#hashCode()} is overwritten here in such a way
	 * that it returns the same hash code for objects with the same content. 
	 * Since the equality check for inserting an object into a Set (HashSet) is based on 
	 * sameness of equals() AND hashCode() (!!)  
	 * <p> 
	 * See <a href="https://stackoverflow.com/questions/6187294/java-set-collection-override-equals-method/11577351">
	 *     https://stackoverflow.com/questions/6187294/java-set-collection-override-equals-method/11577351</a>
	 *     
	 * @see Object#hashCode()    
	 * @see #equals(Object)    
	 */
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
} // class CubeState
   


