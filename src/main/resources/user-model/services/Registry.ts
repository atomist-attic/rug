declare var atomist_registry

class Registry{
  static lookup<T>(id: string): T {
    console.log("Looking up: " + id)
    var res = atomist_registry.lookup(id);
    console.log("Returning:" + res)
    return res
  }
}

export {Registry}
