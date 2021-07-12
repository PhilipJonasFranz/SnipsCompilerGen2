package SEEn.Imm.Formula;

import SEEn.SEState;

public class FBind extends FAbstr {

    public String name, id;

    public FBind(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public boolean eval(SEState state) {
        if (name.equals("result")) {

        }
        else if (name.equals("old")) {

        }

        return false;
    }
}
