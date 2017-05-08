var gulp = require('gulp');
var typedoc = require("gulp-typedoc");

gulp.task("default", ["typedoc"]);

gulp.task("typedoc", function () {
    return gulp
        .src(["."])
        .pipe(typedoc({
            module: "commonjs",
            target: "ES5",
            includeDeclarations: true,
            out: "../../../../typedoc/cortex",
            mode: "file",
            // json: "typedoc/rug.json",
            name: "Atomist",
            // theme: "/path/to/theme",
            excludeExternals: true,
            ignoreCompilerErrors: false,
            version: true,
            hideGenerator: true,
        }));
});
