package edu.ualberta.med.biobank.tools.modelextender;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.ualberta.med.biobank.common.wrappers.Property;
import edu.ualberta.med.biobank.tools.modelumlparser.Attribute;
import edu.ualberta.med.biobank.tools.modelumlparser.ClassAssociation;
import edu.ualberta.med.biobank.tools.modelumlparser.ClassAssociationType;
import edu.ualberta.med.biobank.tools.modelumlparser.ModelClass;
import edu.ualberta.med.biobank.tools.utils.CamelCase;

public class BaseWrapperBuilder extends BaseBuilder {

    protected final String peerpackagename;

    protected final String wrapperPackageName;

    protected Map<String, ModelClass> modelBaseClasses;

    public BaseWrapperBuilder(String outputdir, String packagename,
        String peerpackagename, Map<String, ModelClass> modelClasses) {
        super(outputdir, packagename, modelClasses);
        this.peerpackagename = peerpackagename;

        wrapperPackageName = packagename.replace(".base", "");

        // find all base classes
        modelBaseClasses = new HashMap<String, ModelClass>();
        for (Entry<String, ModelClass> e : modelClasses.entrySet()) {
            ModelClass ec = e.getValue().getExtendsClass();
            if (ec != null) {
                modelBaseClasses.put(ec.getName(), ec);
            }
        }
    }

    private static final Logger LOGGER = Logger
        .getLogger(BaseWrapperBuilder.class.getName());

    protected void generateClassFile(ModelClass mc) throws IOException {
        LOGGER.info("generating peer class for " + mc.getName());

        File f = new File(outputdir + "/" + mc.getName() + "BaseWrapper.java");
        FileOutputStream fos = new FileOutputStream(f);

        StringBuilder contents = new StringBuilder("package ")
            .append(packagename)
            .append(";\n")
            .append("\nimport ")
            .append(Collections.class.getName())
            .append(";\n")
            .append("import ")
            .append(List.class.getName())
            .append(";\n")
            .append("import ")
            .append(Property.class.getName())
            .append(";\n")
            .append("import ")
            .append(wrapperPackageName)
            .append(".*;\n")
            .append("import ")
            .append(wrapperPackageName)
            .append(".internal.*;\n")
            .append(
                "import gov.nih.nci.system.applicationservice.WritableApplicationService;\n");

        contents.append(getWrapperImports(mc));

        // import the peer class
        contents.append("import ").append(peerpackagename).append(".")
            .append(mc.getName()).append("Peer;\n");

        if (modelBaseClasses.containsKey(mc.getName())) {
            contents.append("\npublic abstract class ").append(mc.getName())
                .append("BaseWrapper").append("<E extends ")
                .append(mc.getName()).append("> extends ModelWrapper<E> ");
        } else {
            contents.append("\npublic class ").append(mc.getName())
                .append("BaseWrapper").append(" extends ModelWrapper<")
                .append(mc.getName()).append("> ");
        }
        contents.append("{\n\n");
        contents.append(createContructors(mc));
        contents.append(createRequiredMethods(mc));

        for (Attribute attr : mc.getAttrMap().values()) {
            if (attr.getName().equals("id"))
                continue;
            contents.append(createPropertyGetterAndSetter(mc, attr));
        }

        for (ClassAssociation assoc : mc.getAssocMap().values()) {
            ClassAssociationType assocType = assoc.getAssociationType();
            if ((assocType == ClassAssociationType.ZERO_OR_ONE_TO_ONE)
                || (assocType == ClassAssociationType.ONE_TO_ONE)) {
                contents
                    .append(createWrappedPropertyGetterAndSetter(mc, assoc));
            }
        }

        contents.append("}\n");
        fos.write(contents.toString().getBytes());
    }

    private String createContructors(ModelClass mc) {
        String wrappedObjectType = mc.getName();
        if (modelBaseClasses.containsKey(mc.getName())) {
            wrappedObjectType = "E";
        }

        StringBuilder result = new StringBuilder("    public ")
            .append(mc.getName())
            .append("BaseWrapper(WritableApplicationService appService) {\n")
            .append("        super(appService);\n").append("    }\n\n")
            .append("    public ").append(mc.getName())
            .append("BaseWrapper(WritableApplicationService appService,\n")
            .append("        ").append(wrappedObjectType)
            .append(" wrappedObject) {\n")
            .append("        super(appService, wrappedObject);\n")
            .append("    }\n\n");
        return result.toString();
    }

    private String createRequiredMethods(ModelClass mc) {
        StringBuilder result = new StringBuilder();

        if (!modelBaseClasses.containsKey(mc.getName())) {
            // wrappers for model base classes do not implement the
            // getWrappedClass() method
            result.append("    @Override\n    public Class<");

            ModelClass ec = mc.getExtendsClass();
            if (ec != null) {
                result.append(ec.getName());
            } else {
                result.append(mc.getName());
            }

            result.append("> getWrappedClass() {\n").append("        return ")
                .append(mc.getName()).append(".class;\n").append("    }\n\n");
        }

        result.append("    @Override\n")
            .append("   protected List<String> getPropertyChangeNames() {\n")
            .append("        return ").append(mc.getName())
            .append("Peer.PROP_NAMES;\n").append("    }\n\n");
        return result.toString();

    }

    private String createPropertyGetterAndSetter(ModelClass mc, Attribute member) {
        StringBuilder result = new StringBuilder();
        result.append("   public ").append(member.getType()).append(" get")
            .append(CamelCase.toCamelCase(member.getName(), true))
            .append("() {\n").append("      return getProperty(")
            .append(mc.getName()).append("Peer.")
            .append(CamelCase.toTitleCase(member.getName())).append(");\n")
            .append("   }\n\n");
        return result.toString();
    }

    private String createWrappedPropertyGetterAndSetter(ModelClass mc,
        ClassAssociation assoc) {

        String assocClassName = assoc.getToClass().getName();
        String assocName = assoc.getAssocName();

        StringBuilder result = new StringBuilder();
        result.append("   public ").append(assocClassName)
            .append("Wrapper get")
            .append(CamelCase.toCamelCase(assocName, true)).append("() {\n")
            .append("      return getWrappedProperty(").append(mc.getName())
            .append("Peer.").append(CamelCase.toTitleCase(assocName))
            .append(", ").append(assocClassName).append("Wrapper.class")
            .append(");\n").append("   }\n\n");
        return result.toString();
    }

    protected String getWrapperImports(ModelClass mc) {
        Map<String, Integer> importCount = new HashMap<String, Integer>();

        StringBuilder sb = new StringBuilder();

        for (Attribute attr : mc.getAttrMap().values()) {
            String attrType = attr.getType();
            if (importCount.get(attrType) != null) {
                // already added an import for this class
                continue;
            }

            importCount.put(attrType, 1);
            if (attrType.equals("Date")) {
                sb.append("import ").append(Date.class.getName()).append(";\n");
            }
        }

        boolean hasCollections = false;
        Map<String, ClassAssociation> assocMap = mc.getAssocMap();
        for (ClassAssociation assoc : assocMap.values()) {
            ModelClass toClass = assoc.getToClass();

            if ((assoc.getAssociationType() == ClassAssociationType.ZERO_OR_ONE_TO_MANY)
                || (assoc.getAssociationType() == ClassAssociationType.ONE_TO_MANY)) {
                hasCollections = true;
            }

            if (importCount.get(toClass.getName()) != null) {
                // already added an import for this class
                continue;
            }

            importCount.put(toClass.getName(), 1);
            // sb.append("import ").append(wrapperPackageName).append(".")
            // .append(toClass.getName()).append("Wrapper;\n");
        }

        if (hasCollections) {
            sb.append("import ").append(Collection.class.getName())
                .append(";\n");
        }

        // import the model class itself
        if (!importCount.containsKey(mc.getName())) {
            sb.append("import ").append(mc.getPkg()).append(".")
                .append(mc.getName()).append(";\n");
        }

        // import the model base class
        ModelClass ec = mc.getExtendsClass();
        if (ec != null) {
            sb.append("import ").append(ec.getPkg()).append(".")
                .append(ec.getName()).append(";\n");
        }

        return sb.toString();
    }

}
