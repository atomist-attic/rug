
//used by annotation functions below
function set_metadata(obj: any, key: string, value: any){
  Object.defineProperty(obj, key, {value: value, writable: false, enumerable: false})
}

function get_metadata(obj: any, key: string){
   let desc =  Object.getOwnPropertyDescriptor(obj, key);
   if((desc == null || desc == undefined) && (obj.prototype != undefined)){
     desc = Object.getOwnPropertyDescriptor(obj.prototype, key);
   }
   if(desc != null || desc != undefined){
     return desc.value;
   }
   return null;
}

//exported to annotate rugs
function editor(description :string){
  return function (cons: Function){
    set_metadata(cons,"rug-type","editor");
    set_metadata(cons,"editor-description",description);
  }
}

function generator(description :string){
  return function (cons: Function){
    set_metadata(cons,"rug-type","generator");
    set_metadata(cons,"generator-description",description);
  }
}

function tag(name :string){
  return function (cons: Function){
    let tags: [string] = get_metadata(cons, "tags");
    if(tags == null){
      tags = [name];
    }else if(tags.indexOf(name) < 0){
      tags.push(name)
    }
    set_metadata(cons,"tags",tags);
  }
}

function parameter(details: any){
  return function (target: any, propertyKey: string) {
    let params: {} = get_metadata(target, "params");

    if(params == null){
      params = {}
    }
    params[propertyKey] = details;
    set_metadata(target, "params", params);
  }
}

function inject(typeToInject: string){
   return function (target: any, propertyKey: string, parameterIndex: number) {
       let injects: {}[] = get_metadata(target, "injects");

       if(injects == null){
         injects = []
       }
       injects.push({propertyKey: propertyKey, parameterIndex: parameterIndex, typeToInject: typeToInject})
       set_metadata(target, "injects", injects);
   }
}

function parameters(name: string){
  return function (target: Object, propertyKey: string , parameterIndex: number) {
      set_metadata(target,"parameter-class",name);
  }
}
export {editor}
export {generator}
export {tag}
export {parameter}
export {parameters}
export {inject}
