package rotationsymmetry.sxgboost

import org.scalatest.FunSuite
import rotationsymmetry.sxgboost.loss.SquareLoss

class LossAggregatorSuite extends FunSuite with TestData{

  val loss = new SquareLoss()
  val featureIndicesBundle = Array(Array(0, 1), Array(1, 2))
  val metaData = new MetaData(3, Array(3, 4, 5))
  val tree0 = new WorkingNode(0)
  tree0.prediction = Some(0.4)
  val workingModel = new WorkingModel(0, Array(tree0))

  test("offsets should be of correct size and values") {
    val currentRoot = new WorkingNode(0)
    val agg = new LossAggregator(featureIndicesBundle, workingModel, currentRoot, metaData, loss)
    assert(agg.offsets.length == featureIndicesBundle.length)
    assert(agg.offsets === Array(Array(0, 9), Array(0, 12)))
  }

  test("stats should be of correct size") {
    val currentRoot = new WorkingNode(0)
    val agg = new LossAggregator(featureIndicesBundle, workingModel, currentRoot, metaData, loss)
    assert(agg.stats.length == featureIndicesBundle.length)
    assert(agg.stats(0).length == 21)
    assert(agg.stats(1).length == 27)
  }

  test("add treePoint will not update stats if the leaf node is not in the batch") {
    val currentRoot = new WorkingNode(0)
    val agg = new LossAggregator(featureIndicesBundle, workingModel, currentRoot, metaData, loss)
    val treePoint = TreePoint(0.3, Array(0, 0, 1))
    val stats0 = agg.stats.map(_.clone())
    agg.add(treePoint)
    assert(stats0 === agg.stats)
  }

  test("add treePoint will update stats correctly if the leaf node is in the batch") {
    val currentRoot = new WorkingNode(0)
    currentRoot.idxInBatch = Some(1)
    val agg = new LossAggregator(featureIndicesBundle, workingModel, currentRoot, metaData, loss)
    val treePoint = TreePoint(0.3, Array(0, 0, 1))
    val stats0 = agg.stats.map(_.clone())
    val diff1 = 2 * (0.4 - 0.3)
    val diff2 = 2.0
    val weight = 1.0
    val statsSize = 3
    stats0(1)(0) = diff1
    stats0(1)(1) = diff2
    stats0(1)(2) = weight
    stats0(1)(statsSize * 4 + statsSize * 1) = diff1
    stats0(1)(statsSize * 4 + statsSize * 1 + 1) = diff2
    stats0(1)(statsSize * 4 + statsSize * 1 + 2) = weight
    agg.add(treePoint)
    assert(stats0 === agg.stats)
  }

  test("merge stats correctly") {
    val currentRoot = new WorkingNode(0)
    currentRoot.idxInBatch = Some(0)
    val agg = new LossAggregator(featureIndicesBundle, workingModel, currentRoot, metaData, loss)
    val treePoint1 = TreePoint(0.3, Array(1, 0, 1))
    val treePoint2 = TreePoint(0.4, Array(0, 2, 0))
    agg.add(treePoint1)
    agg.add(treePoint2)
    val agg1 = new LossAggregator(featureIndicesBundle, workingModel, currentRoot, metaData, loss)
    val agg2 = new LossAggregator(featureIndicesBundle, workingModel, currentRoot, metaData, loss)
    agg1.add(treePoint1)
    agg2.add(treePoint2)
    agg1.merge(agg2)
    assert(agg.stats === agg1.stats)
  }

  test("test") {
    val currentRoot = new WorkingNode(0)
    currentRoot.idxInBatch = Some(0)
    val agg = new LossAggregator(Array(Array(0, 1)), new WorkingModel(0.35, Array[WorkingNode]()),
      currentRoot, simpleMetaData, loss)
    simpleBinnedData.foreach(tp => agg.add(tp))
  }
}
