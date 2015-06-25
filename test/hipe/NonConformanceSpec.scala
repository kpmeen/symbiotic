/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import org.specs2.mutable

/**
 * Test scenarios setting up and going through a Non-Conformance process.
 */
class NonConformanceSpec extends mutable.Specification {

  "Creating a NCR process" should {
    "Add a new process" in {
      todo
    }
    "Add step for evaluating NCR" in {
      todo
    }
    "Add private step group for internal review of NCR" in {
      todo
    }
    "Add step to private group for review and comment of the NCR" in {
      todo
    }
    "Add step to private group for approving the NCR" in {
      todo
    }
    "Add step to indicate that an answer is provided for the NCR" in {
      todo
    }
  }

  "Running through NCR's processes" should {
    "Place a new Task in the evaluate step and generate assignment(s)" in {
      todo
    }
    "Allow the NCR-coordinator to REJECT the request" in {
      todo
    }
    "Move the REJECTED task to the 'answer provided' step (generate no assignments)" in {
      todo
    }
    "Allow the NCR-coordinator to ACCEPT the request" in {
      todo
    }
    "Move the ACCEPTED tasks to the review/comment step and generate assignment(s)" in {
      todo
    }
    "Allow a user to COMPLETE a review/comment assignment" in {
      todo
    }
    "Be possible to add additional reviewers by generating more assignments" in {
      todo
    }
    "Be possible to CONSOLIDATE the review/comment task BEFORE the required number of assignments are completed" in {
      todo
    }
    "Be possible to CONSOLIDATE the review/comment task AFTER the required number of assignments are completed" in {
      todo
    }
    "Move the CONSOLIDATED review/comment task to the approval step and generate assignment(s)" in {
      todo
    }
    "Allow a user to RE-REVIEW the CONSOLIDATED task" in {
      todo
    }
    "Move the RE-REVIEW task to the 'review/comment' step and generate assignment(s)" in {
      todo
    }
    "Allow a user to REJECT the CONSOLIDATED task" in {
      todo
    }
    "Move the REJECTED task to the 'answer provided' step and (generate no assignments)" in {
      todo
    }
    "Allow a user to APPROVE the CONSOLIDATED task" in {
      todo
    }
    "Move the APPROVED task to the 'answer provided' step (generate no assignments)" in {
      todo
    }

  }

}
