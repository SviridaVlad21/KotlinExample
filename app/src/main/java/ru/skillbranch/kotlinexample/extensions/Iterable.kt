package ru.skillbranch.kotlinexample.extensions

class Iterable {

    /*Реализуй функцию расширения fun List.dropLastUntil(predicate: (T) -> Boolean): List ,
    в качестве аргумента принимает предикат (лямбда выражение возвращающее Boolean) и возвращат список в котором исключены все элементы с конца до тех пор
    пока не будет выполнено условие предиката (элемент соответствующий условию тоже должен быть исключен из результирующей коллекции)
    (Пример: listOf(1, 2, 3).dropLastUntil{ it==2 } // [1]
    "House Nymeros Martell of Sunspear".split(" ") .dropLastUntil{ it == "of" } // [House, Nymeros, Martell])*/

    fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T>{
        val iterator = listIterator(size)

        while (iterator.hasPrevious()){
            if(predicate(iterator.previous())){
                return take(iterator.nextIndex())
            }
        }
        return emptyList()
    }
}