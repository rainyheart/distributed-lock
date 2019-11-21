package org.rainyheart.distributed.lock.api.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.rainyheart.distributed.lock.api.LockLevel;

@Component
@RestController
@RequestMapping(value = "/aop")
public class DistributedLockTestController {

    @Autowired
    DistributedLockTestService service;
    
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/tryLock", method = RequestMethod.GET)
    @DistributedLock(id = "testId", level = LockLevel.GLOBAL)
    @ResponseBody
    public Result tryLock() {
        System.out.println("tryLock->" + "testId");
        return new Result(1, "testId");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @RequestMapping(value = "/tryLockFailedDueToDuplicatedLockAnnotated", method = RequestMethod.GET)
    @DistributedLock(id = "testId", level = LockLevel.GLOBAL)
    @ResponseBody
    public Result tryLockFailedDueToDuplicatedLockAnnotated() throws Exception {
        System.out.println("tryLockFailedDueToDuplicatedLockAnnotated->" + "testId");
        service.test("testId");
        System.out.println("This should not be printed");
        return new Result(1, "testId");
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/lock", method = RequestMethod.GET)
    @DistributedLock(id = "testId", level = LockLevel.GLOBAL, timeout = 1000)
    @ResponseBody
    public Result lock() {
        System.out.println("lock->" + "testId");
        return new Result(1, "testId");
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/dynamicLock", method = RequestMethod.GET)
    @DistributedLock(id = "#{lock}", level = LockLevel.GLOBAL, timeout = 1000)
    @ResponseBody
    public Result dynamicLock(String lock) {
        System.out.println("dynamicLock->" + lock);
        return new Result(1, lock);
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/dynamicLockParm", method = RequestMethod.POST)
    @DistributedLock(id = "#{parm.value}", level = LockLevel.GLOBAL, timeout = 1000)
    @ResponseBody
    public Result dynamicLockParm(@RequestBody JsonBody parm) {
        System.out.println("dynamicLock->" + parm.getKey() + "=" + parm.getValue());
        return new Result(1, parm.getValue());
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/exception", method = RequestMethod.GET)
    @DistributedLock(id = "#{lock}", level = LockLevel.GLOBAL, timeout = 1000)
    @ResponseBody
    public Result exception(String lock) {
        throw new NullPointerException();
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/testDoubleAnnotation", method = RequestMethod.GET)
    @DistributedLock(id = "#{lock}", level = LockLevel.GLOBAL, timeout = 1000)
    @ResponseBody
    public Result testDoubleAnnotation(String lock) {
        service.test(lock);
        return new Result(1, lock);
    }

    public class Result {
        private int id;
        private String name;

        public Result(int id, String name) {
            super();
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
