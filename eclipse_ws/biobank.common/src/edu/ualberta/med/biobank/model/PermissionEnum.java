package edu.ualberta.med.biobank.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ualberta.med.biobank.common.util.NotAProxy;

/**
 * The id of these enumerations are saved in the database. Therefore, DO NOT
 * CHANGE THESE ENUM IDS (unless you are prepared to write an upgrade script).
 * However, order and enum name can be modified freely.
 * <p>
 * Also, these enums should probably never be deleted, unless they are not used
 * in <em>any</em> database. Instead, they should be deprecated and probably
 * always return false when checking allow-ability.
 * 
 * @author Jonathan Ferland
 * 
 */
public enum PermissionEnum implements NotAProxy, Serializable {
    SPECIMEN_CREATE(2),
    SPECIMEN_READ(3),
    SPECIMEN_UPDATE(4),
    SPECIMEN_DELETE(5),
    SPECIMEN_LINK(6),
    SPECIMEN_ASSIGN(7),

    SITE_CREATE(8, Require.ALL_CENTERS),
    SITE_READ(9),
    SITE_UPDATE(10),
    SITE_DELETE(11),

    PATIENT_CREATE(12),
    PATIENT_READ(13),
    PATIENT_UPDATE(14),
    PATIENT_DELETE(15),
    PATIENT_MERGE(16),

    COLLECTION_EVENT_CREATE(17),
    COLLECTION_EVENT_READ(18),
    COLLECTION_EVENT_UPDATE(19),
    COLLECTION_EVENT_DELETE(20),

    PROCESSING_EVENT_CREATE(21),
    PROCESSING_EVENT_READ(22),
    PROCESSING_EVENT_UPDATE(23),
    PROCESSING_EVENT_DELETE(24),

    ORIGIN_INFO_CREATE(25),
    ORIGIN_INFO_READ(26),
    ORIGIN_INFO_UPDATE(27),
    ORIGIN_INFO_DELETE(28),

    DISPATCH_CREATE(29),
    DISPATCH_READ(30),
    DISPATCH_CHANGE_STATE(31),
    DISPATCH_UPDATE(32),
    DISPATCH_DELETE(33),

    RESEARCH_GROUP_CREATE(34, Require.ALL_CENTERS),
    RESEARCH_GROUP_READ(35),
    RESEARCH_GROUP_UPDATE(36),
    RESEARCH_GROUP_DELETE(37),

    STUDY_CREATE(38, Require.ALL_STUDIES),
    STUDY_READ(39),
    STUDY_UPDATE(40),
    STUDY_DELETE(41),

    REQUEST_CREATE(42),
    REQUEST_READ(43),
    REQUEST_UPDATE(44),
    REQUEST_DELETE(45),

    REQUEST_PROCESS(46),

    CLINIC_CREATE(47, Require.ALL_CENTERS),
    CLINIC_READ(48),
    CLINIC_UPDATE(49),
    CLINIC_DELETE(50),

    CONTAINER_TYPE_CREATE(52),
    CONTAINER_TYPE_READ(53),
    CONTAINER_TYPE_UPDATE(54),
    CONTAINER_TYPE_DELETE(55),

    CONTAINER_CREATE(56),
    CONTAINER_READ(57),
    CONTAINER_UPDATE(58),
    CONTAINER_DELETE(59),

    SPECIMEN_TYPE_CREATE(60, Require.ALL_CENTERS, Require.ALL_STUDIES),
    SPECIMEN_TYPE_READ(61),
    SPECIMEN_TYPE_UPDATE(62, Require.ALL_CENTERS, Require.ALL_STUDIES),
    SPECIMEN_TYPE_DELETE(63, Require.ALL_CENTERS, Require.ALL_STUDIES),

    LOGGING(64),
    REPORTS(65),

    SPECIMEN_LIST(66),
    LABEL_PRINTING(67);

    private static final List<PermissionEnum> VALUES_LIST = Collections
        .unmodifiableList(Arrays.asList(values()));
    private static final Map<Integer, PermissionEnum> VALUES_MAP;

    static {
        Map<Integer, PermissionEnum> map =
            new HashMap<Integer, PermissionEnum>();

        for (PermissionEnum permissionEnum : values()) {
            PermissionEnum check = map.get(permissionEnum.getId());
            if (check != null) {
                throw new RuntimeException("permission enum value "
                    + permissionEnum.getId() + " used multiple times");
            }

            map.put(permissionEnum.getId(), permissionEnum);
        }

        VALUES_MAP = Collections.unmodifiableMap(map);
    }

    private final Integer id;
    private final EnumSet<Require> requires;

    private PermissionEnum(Integer id, Require... requires) {
        this.id = id;
        this.requires = EnumSet.of(Require.DEFAULT, requires);
    }

    public static List<PermissionEnum> valuesList() {
        return VALUES_LIST;
    }

    public static Map<Integer, PermissionEnum> valuesMap() {
        return VALUES_MAP;
    }

    public Integer getId() {
        return id;
    }

    public EnumSet<Require> getRequires() {
        return EnumSet.copyOf(requires);
    }

    public String getName() {
        return name(); // TODO: localized name?
    }

    public static PermissionEnum fromId(Integer id) {
        return valuesMap().get(id);
    }

    /**
     * Whether the given {@link User} has this {@link PermissionEnum} on
     * <em>any</em> {@link Center} or {@link Study}.
     * 
     * @see {@link #isMembershipAllowed(Membership, Center, Study)}
     * @param user
     * @return
     */
    public boolean isAllowed(User user) {
        return isAllowed(user, null, null);
    }

    /**
     * Whether the given {@link User} has this {@link PermissionEnum} on
     * <em>any</em> {@link Center}, but a specific {@link Study}.
     * 
     * @see {@link #isAllowed(User)}
     * @param user
     * @return
     */
    public boolean isAllowed(User user, Study study) {
        return isAllowed(user, null, study);
    }

    /**
     * Whether the given {@link User} has this {@link PermissionEnum} on
     * <em>any</em> {@link Study}, but a specific {@link Center}.
     * 
     * @see {@link #isAllowed(User)}
     * @param user
     * @return
     */
    public boolean isAllowed(User user, Center center) {
        return isAllowed(user, center, null);
    }

    /**
     * 
     * @param user
     * @param center if null, {@link Center} does not matter.
     * @param study if null, {@link Study} does not matter.
     * @return
     */
    public boolean isAllowed(User user, Center center, Study study) {
        for (Membership m : user.getAllMemberships()) {
            if (isMembershipAllowed(m, center, study)) return true;
        }
        return false;
    }

    /**
     * Return true if special requirements (i.e. {@link Require}-s) are met on
     * the given {@link Membership}, otherwise false.
     * 
     * @param m
     * @return
     */
    public boolean isRequirementsMet(Membership m) {
        boolean reqsMet = true;
        Domain d = m.getDomain();
        reqsMet &= !requires.contains(Require.ALL_CENTERS) || d.isAllCenters();
        reqsMet &= !requires.contains(Require.ALL_STUDIES) || d.isAllStudies();
        return reqsMet;
    }

    /**
     * This is a confusing check. If {@link Center} is null, it means we do not
     * care about its value, otherwise, {@link Domain#contains(Center)} must be
     * true. The same applies to {@link Study}.
     * 
     * @param m
     * @param c
     * @param s
     * @return
     */
    private boolean isMembershipAllowed(Membership m, Center c, Study s) {
        boolean requiresMet = isRequirementsMet(m);
        boolean hasCenter = c == null || m.getDomain().contains(c);
        boolean hasStudy = s == null || m.getDomain().contains(s);
        boolean hasPermission = m.getAllPermissions().contains(this);

        boolean allowed = requiresMet && hasCenter && hasStudy && hasPermission;
        return allowed;
    }

    /**
     * Defines special requirements for a {@link PermissionEnum}.
     * 
     * @author jferland
     * 
     */
    public enum Require implements NotAProxy, Serializable {
        /**
         * Does nothing but make creating {@link EnumSet}-s easier.
         */
        DEFAULT,

        /**
         * If present, the {@link PermissionEnum} must exist in a
         * {@link Membership} for which its {@link Domain#isAllCenters()}
         * returns true.
         */
        ALL_CENTERS,

        /**
         * If present, the {@link PermissionEnum} must exist in a
         * {@link Membership} for which its {@link Domain#isAllStudies()}
         * returns true.
         */
        ALL_STUDIES;
    }
}