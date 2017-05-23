"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
exports.__esModule = true;
var Decorators_1 = require("@atomist/rug/operations/Decorators");
var FruitererType = (function () {
    function FruitererType() {
        this.typeName = "fruiterer";
    }
    FruitererType.prototype.isFile = function (f) {
        return true;
    };
    FruitererType.prototype.find = function (context) {
        if (this.isFile(context)) {
            var f = context;
            if (f.isJava)
                return [new Fruiterer(f)];
            else
                return [];
        }
        else
            return [];
    };
    return FruitererType;
}());
var Fruiterer = (function () {
    function Fruiterer(file) {
        this.file = file;
    }
    Fruiterer.prototype.parent = function () { return this.file; };
    Fruiterer.prototype.nodeName = function () { return "fruiterer"; };
    Fruiterer.prototype.nodeTags = function () { return [this.nodeName()]; };
    Fruiterer.prototype.children = function () { return [new MutatingBanana(this.file), new Pear()]; };
    return Fruiterer;
}());
var MutatingBanana = (function () {
    function MutatingBanana(file) {
        this.file = file;
    }
    MutatingBanana.prototype.parent = function () { return this.file; };
    MutatingBanana.prototype.nodeName = function () { return "mutatingBanana"; };
    MutatingBanana.prototype.nodeTags = function () { return [this.nodeName()]; };
    MutatingBanana.prototype.children = function () { return []; };
    MutatingBanana.prototype.mutate = function () {
        this.file.prepend("I am evil");
    };
    return MutatingBanana;
}());
var Pear = (function () {
    function Pear() {
    }
    Pear.prototype.parent = function () { return null; };
    Pear.prototype.nodeName = function () { return "pear"; };
    Pear.prototype.nodeTags = function () { return [this.nodeName()]; };
    Pear.prototype.children = function () { return []; };
    return Pear;
}());
var TwoLevel = (function () {
    function TwoLevel() {
    }
    TwoLevel.prototype.edit = function (project) {
        var mg = new FruitererType();
        var eng = project.context.pathExpressionEngine.addType(mg);
        eng["with"](project, "//File()/fruiterer()/mutatingBanana()", function (n) {
            n.mutate();
        });
    };
    return TwoLevel;
}());
TwoLevel = __decorate([
    Decorators_1.Editor("Constructed", "Uses two dynamic grammars")
], TwoLevel);
exports.editor = new TwoLevel();
