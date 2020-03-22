package scala.net.drozdowicz.sxacml;

import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BooleanAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.cond.Evaluatable;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.cond.FunctionBase;
import org.wso2.balana.ctx.EvaluationCtx;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by drozd on 22.03.2020.
 */
public class BoolTextCompare extends FunctionBase {

    // the name of the function, which will be used publicly
    public static final String NAME = "bool-text-compare";

    // the parameter types, in order, and whether or not they're bags
    private static final String params [] = { StringAttribute.identifier,
            BooleanAttribute.identifier };
    private static final boolean bagParams [] = { false, false };

    public BoolTextCompare() {
        // use the constructor that handles mixed argument types
        super(NAME, 0, params, bagParams, BooleanAttribute.identifier,
                false);
    }

    public static Set getSupportedIdentifiers() {

        Set set = new HashSet();
        set.add(NAME);
        return set;
    }


    @Override
    public EvaluationResult evaluate(List<Evaluatable> inputs, EvaluationCtx context){
        // Evaluate the arguments using the helper method...this will
        // catch any errors, and return values that can be compared
        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        // cast the resolved values into specific types
        StringAttribute str = (StringAttribute)(argValues[0]);
        BooleanAttribute bool = (BooleanAttribute)(argValues[1]);
        boolean evalResult;

        // now compare the values
        if (bool.getValue()) {
            // see if the string is "true"
            evalResult = str.getValue().equals("true");
        } else {
            // see if the string is "false"
            evalResult = str.getValue().equals("false");
        }

        // boolean returns are common, so there's a getInstance() for that
        return EvaluationResult.getInstance(evalResult);
    }
}