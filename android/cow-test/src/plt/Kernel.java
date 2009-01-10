package plt;
// The Kernel class contains all of the builtins.


import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import plt.types.*;
import plt.gui.*;

public class Kernel {

    static private java.util.Random randgen = new java.util.Random();


    // no-op: void -> void
    public static Object no_op() {
	return null;
    }

    public static Object no_op_worldEvent(Object x) {
	return x;
    }

    public static Object no_op_stopWhen(Object x) {
	return Logic.FALSE;
    }

    public static Object no_op_keyEvent(Object x, Object key) {
	return x;
    }

    //////////////////////////////////////////////////////////////////////

    public static plt.types.Number pi = FloatPoint.PI;
    public static plt.types.Number e = FloatPoint.E;



    public static Object identity(Object o) {
	return o;
    }

    // Numerics

    // >=
    public static Logic _greaterthan__equal_(Object _n1, Object _n2) {
	return toLogic(NumberTower.greaterThanEqual((plt.types.Number) _n1,
						    (plt.types.Number) _n2));
    }
    
    // >
    public static Logic _greaterthan_(Object _n1, Object _n2) {
	return toLogic(NumberTower.greaterThan((plt.types.Number) _n1,
					       (plt.types.Number) _n2));
    }
    
    // <=
    public static Logic _lessthan__equal_(Object _n1, Object _n2) {
	return toLogic(NumberTower.lessThanEqual((plt.types.Number) _n1,
						 (plt.types.Number) _n2));
    }
    
    // <
    public static Logic _lessthan_(Object _n1, Object _n2) {
	return toLogic(NumberTower.lessThan((plt.types.Number)_n1, 
					    (plt.types.Number)_n2));
    }


    // =
    public static Logic _equal_(Object _n1, Object _n2) {
	return toLogic(NumberTower.equal((plt.types.Number)_n1, 
					 (plt.types.Number)_n2));
    }


    // =~
    public static Logic _equal__tilde_(Object _n1, Object _n2, Object _n3) {
	return toLogic(NumberTower.approxEqual((plt.types.Number)_n1,
					       (plt.types.Number)_n2,
					       (plt.types.Number)_n3));
    }


    // +
    public static plt.types.Number _plus_(Object _n1, Object _n2) {
	return NumberTower.plus((plt.types.Number)_n1, (plt.types.Number)_n2);
    }


    // -
    public static plt.types.Number _dash_(Object _n1, Object _n2) {
	return NumberTower.minus((plt.types.Number)_n1, (plt.types.Number)_n2);
    }


    // *
    public static plt.types.Number _star_(Object _n1, Object _n2) {
	return NumberTower.multiply((plt.types.Number)_n1, (plt.types.Number)_n2);
    }

    // /
    public static plt.types.Number _slash_(Object _n1, Object _n2) {
	return NumberTower.divide((plt.types.Number)_n1, (plt.types.Number)_n2);
    }


    public static plt.types.Number abs(Object n) {
	return ((plt.types.Number)n).abs();
    }


    public static plt.types.Number acos(Object n) {
	return ((plt.types.Number)n).acos();
    }


    public static plt.types.Number sqrt(Object _n) {
	plt.types.Number n = (plt.types.Number) _n;
	return ((plt.types.Number)n).sqrt();
    }

    public static plt.types.Number modulo(Object _n1, Object _n2) {
	plt.types.Number n1 = (plt.types.Number) _n1;
	plt.types.Number n2 = (plt.types.Number) _n2;

	if (NumberTower.coerseLeft(n1, Rational.ONE) != null) {
	    n1 = NumberTower.coerseLeft(n1, Rational.ONE);
	}
	if (NumberTower.coerseLeft(n2, Rational.ONE) != null) {
	    n2 = NumberTower.coerseLeft(n2, Rational.ONE);
	}
	
	return n1.modulo(n2);
    }


    public static plt.types.Number floor(Object _n1) {
	return ((plt.types.Number)_n1).floor();
    }

    public static plt.types.Number ceiling(Object _n1) {
	return ((plt.types.Number)_n1).ceiling();
    }

    public static plt.types.Number sin(Object _n1) {
	return ((plt.types.Number)_n1).sin();
    }

    public static plt.types.Number asin(Object _n1) {
	return ((plt.types.Number)_n1).asin();
    }

    public static plt.types.Number atan(Object _n1) {
	return ((plt.types.Number)_n1).atan();
    }


    public static plt.types.Number cos(Object _n1) {
	return ((plt.types.Number)_n1).cos();
    }

    public static String number_dash__greaterthan_string(Object _n1) {
	return ((plt.types.Number)_n1).toString();
    }

    public static Logic equal_question_(Object _o1, Object _o2) {
	if (_o1 instanceof plt.types.Number && _o2 instanceof plt.types.Number) {
	    return _equal_(_o1, _o2);
	}
	return toLogic(_o1.equals(_o2));
    }


    //////////////////////////////////////////////////////////////////////
    public static plt.types.Number random(Object n) {
	int result = randgen.nextInt() % ((plt.types.Number)n).toInt();
	// Oddity, but modulo can return negative if dividend is negative.
	if (result < 0) {
	    return new Rational(result + ((plt.types.Number)n).toInt(),
				1);
	} else {
	    return new Rational(result, 1);
	}
    }

    public static Logic zero_question_(java.lang.Object n) {
	return toLogic(((plt.types.Number)n).isZero());
    }
    
    public static plt.types.Number max(Object n1, Object n2) {
	if (_greaterthan__equal_(n1, n2)

	    .isTrue()) {
	    return (plt.types.Number) n1;
	}
	return (plt.types.Number) n2;
    }

    public static plt.types.Number min(Object n1, Object n2) {
	if (_lessthan__equal_(n1, n2)
	    .isTrue()) {
	    return (plt.types.Number) n1;
	}
	return (plt.types.Number) n2;
    }

    public static plt.types.Number sqr(Object n) {
	return _star_(n, n);
    }


    public static plt.types.Number add1(Object n) {
	return _plus_(n, Rational.ONE);
    }

    public static plt.types.Number sub1(Object n) {
	return _dash_(n, Rational.ONE);
    }



    //////////////////////////////////////////////////////////////////////
    public static Logic string_equal__question_(Object s1, Object s2) {
	return toLogic(((String)s1).equals(s2));
    }


    public static Logic struct_question_(Object obj) {
	return toLogic(obj instanceof plt.types.Struct);
    }

    //////////////////////////////////////////////////////////////////////
    // Posn stuff
    public static Posn make_dash_posn(Object x, Object y) {
	return new Posn(x, y);
    }
    public static Object posn_dash_x(Object p) {
	return ((Posn)p).getX();
    }
    public static Object posn_dash_y(Object p) {
	return ((Posn)p).getY();
    }
    public static Logic posn_question_(Object p) {
	return toLogic(p instanceof plt.types.Posn);
    }


    //////////////////////////////////////////////////////////////////////

    public static plt.types.Number image_dash_width(Object img) {
	return new Rational(((Picture)img).getWidth(), 1);
    }


    public static plt.types.Number image_dash_height(Object img) {
	return new Rational(((Picture)img).getHeight(), 1);
    }
    


    // World kernel functions
    public static Scene place_dash_image(Object image,
					 Object x,
					 Object y,
					 Object scene) {
	return ((Scene)scene).placeImage((Picture)image,
					 ((plt.types.Number) x).toInt(),
					 ((plt.types.Number) y).toInt());
    }

    
    public static Scene empty_dash_scene(Object width, Object height) {
	return Scene.emptyScene(((plt.types.Number)width).toInt(),
				((plt.types.Number)height).toInt());
    }


    public static Picture text(Object s, Object size, Object color) {
	return new TextPicture((String)s,
			       ((plt.types.Number)size).toInt(),
			       Color.lookup(coerseToString(color)));
    }

    
    public static Picture circle(Object radius, Object style, Object color) {
	return new CirclePicture(((plt.types.Number)radius).toInt(),
				 coerseToString(style),
				 Color.lookup(coerseToString(color)));
    }
    
    public static Picture nw_colon_rectangle(Object width, Object height, 
					     Object style, Object color) {
	return new NwRectanglePicture(((plt.types.Number)width).toInt(),
				      ((plt.types.Number)height).toInt(),
				      coerseToString(style),
				      Color.lookup(coerseToString(color)));
    }

    public static Picture rectangle(Object width, Object height, 
				    Object style, Object color) {
	return new RectanglePicture(((plt.types.Number)width).toInt(),
				    ((plt.types.Number)height).toInt(),
				    coerseToString(style),
				    Color.lookup(coerseToString(color)));
    }


    // Loads up the image resource named by filename.
    // FIXME: we still don't have a good way to prevent the user from
    // colliding with this name accidently...
    public static Picture _dash_kernel_dash_create_dash_image(Object filename) {
	return new FilePicture((String) filename);
    }

    //////////////////////////////////////////////////////////////////////

    public static Object first(Object l) {
	return ((plt.types.List)l).first();
    }

    public static Object second(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	return l.rest().first();
    }
    
    public static Object third(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	return l.rest().rest().first();
    }
    public static Object fourth(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	return l.rest().rest().rest().first();
    }
    public static Object fifth(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	return l.rest().rest().rest().rest().first();
    }
    public static Object sixth(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	return l.rest().rest().rest().rest().rest().first();
    }
    public static Object seventh(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	return l.rest().rest().rest().rest().rest().rest().first();
    }
    public static Object eighth(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	return l.rest().rest().rest().rest().rest().rest().rest().first();
    }


    public static plt.types.List reverse(Object _l) {
	plt.types.List l = (plt.types.List) _l;
	plt.types.List rev = plt.types.Empty.EMPTY;
	while (! l.isEmpty()) {
	    rev = cons(l.first(), rev);
	    l = l.rest();
	}
	return rev;
    }



    public static plt.types.List rest(Object l) {
	return ((plt.types.List)l).rest();
    }

    public static Object car(Object o) {
	return first(o);
    }
    public static plt.types.List cdr(Object o) {
	return rest(o);
    }

    public static Object caaar(Object o) {
	return car(car(car(o)));
    }
    
    public static Object caadr(Object o) {
	return car(car(cdr(o)));
    }

    public static Object caar(Object o) {
	return car(car(o));
    }

    public static Object cadar(Object o) {
	return car(cdr(car(o)));
    }
    
    public static Object cadddr(Object o) {
	return car(cdr(cdr(cdr(o))));
    }

    public static Object caddr(Object o) {
	return car(cdr(cdr(o)));
    }

    public static Object cadr(Object o) {
	return car(cdr(o));
    }

    public static plt.types.List cdaar(Object o) {
	return cdr(car(car(o)));
    }
    public static plt.types.List cdadr(Object o) {
	return cdr(car(cdr(o)));
    }

    public static plt.types.List cdar(Object o) {
	return cdr(car(o));
    }

    public static plt.types.List cddar(Object o) {
	return cdr(cdr(car(o)));
    }

    public static plt.types.List cdddr(Object o) {
	return cdr(cdr(cdr(o)));
    }

    public static plt.types.List cddr(Object o) {
	return cdr(cdr(o));
    }


    public static Logic empty_question_(Object l) {
	return toLogic(((plt.types.List)l).isEmpty());
    }

    public static plt.types.List cons(Object x, Object xs) {
	return new Pair(x, (plt.types.List)xs);
    }

    //////////////////////////////////////////////////////////////////////

    public static Object key_equal__question_(Object k1, Object k2) {
	return toLogic(k1.toString().toUpperCase().equals
		       (k2.toString().toUpperCase()));

    }


    //////////////////////////////////////////////////////////////////////
    public static Logic symbol_equal__question_(Object s1, Object s2) {
	return toLogic(((Symbol)s1).equals(s2));
    }


    public static String symbol_dash__greaterthan_string(Object o) {
	return ((Symbol)o).toString();
    }


    public static Logic not(Object l) {
	return ((Logic) l).negate();
    }

    
    //////////////////////////////////////////////////////////////////////
    
    public static Object error(Object s, Object msg) {
	throw new RuntimeException(s + ": " + msg);
    }

    public static Object error(Object s) {
	throw new RuntimeException("" + s);
    }

    
    //////////////////////////////////////////////////////////////////////

    // Converts from boolean to Logics.
    private static Logic toLogic(boolean b) {
	return b ? Logic.TRUE : Logic.FALSE;
    }



    // Coerses a symbol or string into a string.
    private static String coerseToString(Object obj) {
	if (obj instanceof Symbol) {
	    return ((Symbol)obj).toString();
	} else {
	    return (String) obj;
	}
    }

}
