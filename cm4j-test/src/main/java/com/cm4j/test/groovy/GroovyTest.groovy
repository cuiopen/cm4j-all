class T {
    def calc(String str) {
        str.toInteger()
    }
    def calc(int x, int y) {
        x + y
    }
}
T ref = new T()
assert 10 == ref.&calc('10')    // 这种属于直接方法调用
def calc = ref.&calc;           // 本质就是方法引用
assert 10 == calc("10")         // 方法闭包也可以重载
assert 10 == calc(4 , 6)
