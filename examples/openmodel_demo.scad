$fn = 48;

module rounded_block(size = [64, 38, 8], radius = 4) {
    hull() {
        for (x = [-size.x / 2 + radius, size.x / 2 - radius])
            for (y = [-size.y / 2 + radius, size.y / 2 - radius])
                translate([x, y, 0]) cylinder(h = size.z, r = radius, center = true);
    }
}

difference() {
    union() {
        rounded_block();
        translate([0, 12, 14]) rotate([90, 0, 0])
            cylinder(h = 8, r = 15, center = true);
        translate([-23, -4, 12]) cube([8, 18, 24], center = true);
        translate([23, -4, 12]) cube([8, 18, 24], center = true);
    }

    translate([0, 12, 14]) rotate([90, 0, 0])
        cylinder(h = 12, r = 9, center = true);

    for (x = [-24, 24])
        translate([x, 0, 0]) cylinder(h = 16, r = 2.2, center = true);
}
