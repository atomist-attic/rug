abstract class Animal(name: String)

class Dog(name: String) extends Animal(name)
class Cat(name: String) extends Animal(name)
class Bird(name: String) extends Animal(name)


val as = Set(new Dog("fido"), new Cat("stinky"))

val cs = Set(classOf[Dog], classOf[Cat], classOf[Bird])
