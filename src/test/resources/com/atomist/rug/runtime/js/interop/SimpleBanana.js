"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
exports.__esModule = true;
var Decorators_1 = require("@atomist/rug/operations/Decorators");
var BananaType = (function () {
    function BananaType() {
        this.typeName = "banana";
    }
    BananaType.prototype.find = function (context) {
        return [new Banana()];
    };
    return BananaType;
}());
var Banana = (function () {
    function Banana() {
    }
    Banana.prototype.parent = function () {
        return null;
    };
    Banana.prototype.nodeName = function () {
        return "banana";
    };
    Banana.prototype.nodeTags = function () {
        return [this.nodeName()];
    };
    Banana.prototype.value = function () {
        return "yellow";
    };
    Banana.prototype.children = function () {
        return [];
    };
    return Banana;
}());
var SimpleBanana = (function () {
    function SimpleBanana() {
    }
    SimpleBanana.prototype.edit = function (project) {
        var mg = new BananaType();
        var eng = project.context.pathExpressionEngine.addType(mg);
        var i = 0;
        eng["with"](project, "//File()/banana()", function (n) {
            //console.log("Checking color of banana")
            if (n.value() != "yellow")
                throw new Error("Banana is not yellow but [" + n.value() + "]. Sad.");
            i++;
        });
        if (i == 0)
            throw new Error("No bananas tested. Sad.");
    };
    return SimpleBanana;
}());
SimpleBanana = __decorate([
    Decorators_1.Editor("Constructed", "Uses single dynamic grammar")
], SimpleBanana);
exports.editor = new SimpleBanana();
