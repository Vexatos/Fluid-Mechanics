package liquidmechanics.api.helpers;


public enum ColorCode
{
    BLACK("Black"),
    RED("Red"),
    GREEN("Green"),
    BROWN("Brown"),
    BLUE("Blue"),
    PURPLE("Purple"),
    CYAN("Cyan"),
    SILVER("Silver"),
    GREY("Grey"),
    PINK("Pink"),
    LIME("Lime"),
    YELLOW("Yellow"),
    LIGHTBLUE("LightBlue"),
    WHITE("White"),
    ORANGE("Orange"),
    NONE("");
    
    String name;

    private ColorCode(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    /** gets a pipeColor from any of the following
     * 
     * @param obj
     *            - Integer,String,LiquidData,PipeColor
     * @return Color NONE if it can't find it */
    public static ColorCode get(Object obj)
    {
        if (obj instanceof Integer && ((Integer) obj) < ColorCode.values().length)
        {
            return ColorCode.values()[((Integer) obj)];
        } else if (obj instanceof LiquidData)
        {
            return ((LiquidData) obj).getColor();
        } else if (obj instanceof ColorCode)
        {
            return (ColorCode) obj;
        } else if (obj instanceof String)
        {
            for (int i = 0; i < ColorCode.values().length; i++)
            {
                if (((String) obj).equalsIgnoreCase(ColorCode.get(i).getName())) { return ColorCode.get(i); }
            }
        }
        return NONE;
    }

    /** gets the liquidData linked with this color. in rare cases there could be
     * more than one, but first instance will be returned */
    public LiquidData getLiquidData()
    {
        for (LiquidData data : LiquidHandler.allowedLiquids)
        {
            if (data.getColor() == this) { return data; }
        }
        return LiquidHandler.unkown;
    }
}