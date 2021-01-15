package compiler.valhalla.framework;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Random;

class Argument {
    private static final Random random = new Random();

    final private Object argumentValue;
    final private boolean isToggleBoolean;
    final private boolean isRandomEach;
    final private boolean isFixedRandom;
    final private Class<?> randomClass;
    private boolean previousBoolean;

    private Argument(Object argumentValue, Boolean booleanToggle, boolean isFixedRandom) {
        this.argumentValue = argumentValue;
        this.isToggleBoolean = booleanToggle != null;
        this.previousBoolean = isToggleBoolean && !booleanToggle;
        this.isRandomEach = false;
        this.randomClass = null;
        this.isFixedRandom = isFixedRandom;
    }

    private Argument(Object argumentValue, Class<?> randomClass) {
        this.argumentValue = argumentValue;
        this.isToggleBoolean = false;
        this.isRandomEach = true;
        this.randomClass = randomClass;
        this.isFixedRandom = false;
    }

    /**
     * Return all arguments for the @Arguments annotation.
     * @param m The @Test method
     * @return  Return array with Argument objects for each specified argument in the @Arguments annotation of m.
     *          Return null if method has no @Arguments annotation.
     */
    public static Argument[] getArguments(Method m) {
        Arguments argumentsAnno = m.getAnnotation(Arguments.class);
        if (argumentsAnno == null) {
            return null;
        }
        ArgumentValue[] values = argumentsAnno.value();
        Argument[] arguments = new Argument[values.length];
        Class<?>[] declaredParameters = m.getParameterTypes();
        TestFormat.check(values.length == declaredParameters.length,
                         "Number of argument values provided in @Arguments does not match the number of actual arguments in " + m);

        for (int i = 0; i < values.length; i++) {
            ArgumentValue specifiedArg = values[i];
            Class<?> parameter = declaredParameters[i];
            switch (specifiedArg) {
                case DEFAULT -> arguments[i] = createDefault(parameter);
                case NUMBER_42 -> {
                    TestFormat.check(isNumber(parameter),
                                     "Provided invalid NUMBER_42 argument for non-number " + parameter + " for " + m);
                    arguments[i] = create((byte) 42);
                }
                case NUMBER_MINUS_42 -> {
                    TestFormat.check(isNumber(parameter),
                                     "Provided invalid NUMBER_MINUS_42 argument for non-number " + parameter + " for " + m);
                    arguments[i] = create((byte) -42);
                }
                case BOOLEAN_TOGGLE_FIRST_FALSE -> {
                    TestFormat.check(isBoolean(parameter),
                                     "Provided invalid BOOLEAN_TOGGLE_FIRST_FALSE argument for non-boolean " + parameter + " for " + m);
                    arguments[i] = createToggleBoolean(false);
                }
                case BOOLEAN_TOGGLE_FIRST_TRUE -> {
                    TestFormat.check(Argument.isBoolean(parameter),
                                     "Provided invalid BOOLEAN_TOGGLE_FIRST_TRUE argument for non-boolean " + parameter + " for " + m);
                    arguments[i] = createToggleBoolean(true);
                }
                case TRUE -> {
                    TestFormat.check(Argument.isBoolean(parameter),
                                     "Provided invalid TRUE argument for non-boolean " + parameter + " for " + m);
                    arguments[i] = create(true);
                }
                case FALSE -> {
                    TestFormat.check(Argument.isBoolean(parameter),
                                     "Provided invalid FALSE argument for non-boolean " + parameter + " for " + m);
                    arguments[i] = create(false);
                }
                case RANDOM_ONCE -> {
                    TestFormat.check(isPrimitiveType(parameter),
                                     "Provided invalid RANDOM_ONCE argument for non-primitive type " + parameter + " for " + m);
                    arguments[i] = createRandom(parameter);
                }
                case RANDOM_EACH -> {
                    TestFormat.check(isPrimitiveType(parameter),
                                     "Provided invalid RANDOM_ONCE argument for non-primitive type " + parameter + " for " + m);
                    arguments[i] = createRandomEach(parameter);
                }
            }
        }
        return arguments;
    }

    private static Argument create(Object argumentValue) {
        return new Argument(argumentValue,  null,false);
    }

    private static Argument createDefault(Class<?> c) {
        if (Argument.isNumber(c)) {
            return Argument.create((byte)0);
        } else if (Argument.isChar(c)) {
            return Argument.create('\u0000');
        } else if (Argument.isBoolean(c)) {
            return Argument.create(false);
        } else {
            // Object
            try {
                Constructor<?> constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                return Argument.create(constructor.newInstance());
            } catch (Exception e) {
                TestFormat.fail("Cannot create new default instance of " + c, e);
                return null;
            }
        }
    }

    private static Argument createRandom(Class<?> c) {
        return new Argument(getRandom(c), null, true);
    }
    private static Argument createToggleBoolean(boolean firstBoolean) {
        return new Argument(null, firstBoolean, false);
    }

    private static Argument createRandomEach(Class<?> c) {
        return new Argument(null, c);
    }

    public boolean isFixedRandom() {
        return isFixedRandom;
    }

    public Object getArgument() {
        if (isToggleBoolean) {
            previousBoolean = !previousBoolean;
            return previousBoolean;
        } else if (isRandomEach) {
            return getRandom(randomClass);
        } else {
            return argumentValue;
        }
    }

    private static boolean isPrimitiveType(Class<?> c) {
        return isNumber(c) || isBoolean(c) || isChar(c);
    }

    private static boolean isBoolean(Class<?> c) {
        return c.equals(boolean.class);
    }

    private static boolean isChar(Class<?> c) {
        return c.equals(char.class);
    }
    private static boolean isNumber(Class<?> c) {
        return isIntNumber(c) || isFloatNumber(c);
    }

    private static boolean isIntNumber(Class<?> c) {
        return c.equals(byte.class)
                || c.equals(short.class)
                || c.equals(int.class)
                || c.equals(long.class);
    }

    public static boolean isFloatNumber(Class<?> c) {
        return c.equals(float.class) || c.equals(double.class);
    }

    private static Object getRandom(Class<?> c) {
        if (isBoolean(c)) {
            return random.nextBoolean();
        } else if (c.equals(byte.class)) {
            return (byte)random.nextInt(256);
        } else if (isChar(c)) {
            return (char)random.nextInt(65536);
        }  else if (c.equals(short.class)) {
            return (short)random.nextInt(65536);
        } else if (c.equals(int.class)) {
            return random.nextInt();
        } else if (c.equals(long.class)) {
            return random.nextLong();
        } else if (c.equals(float.class)) {
            // Get number between 0 and 1000.
            return random.nextFloat() * 1000;
        } else if (c.equals(double.class)) {
            // Get number between 0 and 1000.
            return random.nextDouble() * 1000;
        } else {
            TestFormat.fail("Cannot generate random value for non-primitive type");
            return null;
        }
    }
}
